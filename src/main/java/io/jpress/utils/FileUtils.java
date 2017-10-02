/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.jfinal.core.JFinal;
import com.jfinal.kit.PathKit;
import com.jfinal.weixin.sdk.utils.IOUtils;


public class FileUtils {

	public static String getSuffix(String fileName) {
		if (fileName != null && fileName.contains(".")) {
			return fileName.substring(fileName.lastIndexOf("."));
		}
		return null;
	}

	public static Long FILE_COPY_BUFFER_SIZE = 1024L * 1024 * 30;

	public static boolean copyFile(String src,String dest){
		File srcFile = new File(src);
		if(!srcFile.exists() || srcFile.isDirectory()){
			return false;
		}
		File destFile = new File(dest);
		if(destFile.exists()){
			return false;
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel input = null;
		FileChannel output = null;
		try {
			fis = new FileInputStream(srcFile);
			fos = new FileOutputStream(destFile);
			input = fis.getChannel();
			output = fos.getChannel();
			final long size = input.size(); // TODO See IO-386
			long pos = 0;
			long count = 0;
			while (pos < size) {
				final long remain = size - pos;
				count = remain > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : remain;
				final long bytesCopied = output.transferFrom(input, pos, count);
				if (bytesCopied == 0) { // IO-385 - can happen if file is truncated after caching the size
					break; // ensure we don't loop forever
				}
				pos += bytesCopied;
			}
		} catch (IOException e){
			return false;
		} finally {
			closeQuietly(output, fos, input, fis);
		}
		return true;
	}

	private static void closeQuietly(FileChannel fc1,FileOutputStream output,FileChannel fc2,FileInputStream input){
		try {
			if(fc1 != null) {
				fc1.close();
			}
			if(output != null) {
				output.close();
			}
			if(fc2 != null) {
				fc2.close();
			}
			if(input != null) {
				input.close();
			}
		} catch (IOException e){

		}
	}
	
	
	public static String removePrefix(String src, String prefix) {
		if (src != null && src.startsWith(prefix)) {
			return src.substring(prefix.length());
		}
		return src;
	}
	
	
	public static String removeRootPath(String src){
		return removePrefix(src, PathKit.getWebRootPath());
	}

	public static String readString(File file) {
		ByteArrayOutputStream baos = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int len = 0; (len = fis.read(buffer)) > 0;) {
				baos.write(buffer, 0, len);
			}
			return new String(baos.toByteArray(),JFinal.me().getConstants().getEncoding());
		} catch (Exception e) {
		} finally {
			close(fis, baos);
		}
		return null;
	}

	public static void writeString(File file, String string) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
			fos.write(string.getBytes(JFinal.me().getConstants().getEncoding()));
		} catch (Exception e) {
		} finally {
			close(null, fos);
		}
	}

	private static void close(InputStream is, OutputStream os) {
		if (is != null)
			try {
				is.close();
			} catch (IOException e) {
			}
		if (os != null)
			try {
				os.close();
			} catch (IOException e) {
			}
	}

	public static void unzip(String zipFilePath) throws IOException {
		String targetPath = zipFilePath.substring(0, zipFilePath.lastIndexOf("."));
		unzip(zipFilePath, targetPath);
	}

	public static void unzip(String zipFilePath, String targetPath) throws IOException {
		ZipFile zipFile = new ZipFile(zipFilePath);
		try{
			Enumeration<?> entryEnum = zipFile.entries();
			if (null != entryEnum) {
				while (entryEnum.hasMoreElements()) {
					OutputStream os = null;
					InputStream is = null;
					try {
						ZipEntry zipEntry = (ZipEntry) entryEnum.nextElement();
						if (!zipEntry.isDirectory()) {
							File targetFile = new File(targetPath + File.separator + zipEntry.getName());
							if (!targetFile.getParentFile().exists()) {
								targetFile.getParentFile().mkdirs();
							}
							os = new BufferedOutputStream(new FileOutputStream(targetFile));
							is = zipFile.getInputStream(zipEntry);
							byte[] buffer = new byte[4096];
							int readLen = 0;
							while ((readLen = is.read(buffer, 0, 4096)) > 0) {
								os.write(buffer, 0, readLen);
							}
						}
					} finally {
						if (is != null)
							is.close();
						if (os != null)
							os.close();
					}
				}
			}
		}finally{
			zipFile.close();
		}
	}

}