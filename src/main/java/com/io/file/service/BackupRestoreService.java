package com.io.file.service;

import com.google.gson.Gson;
import com.io.file.model.UserAddressDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BackupRestoreService {
    private final String ZIP_TYPE = ".zip";
    private final Function<String, String> fileNameDotZip = fileName -> fileName.toLowerCase().endsWith(ZIP_TYPE) ? fileName : fileName + ZIP_TYPE;
    private String backupFolder;

    private String backupFolder() {
        String BASE_PATH = "/text/sample";
        if (Objects.isNull(backupFolder)) backupFolder = BASE_PATH + "/backup/";
        return backupFolder;
    }

    private File zipFile(File fileName) {
        File destinationFolder = new File(backupFolder());
        if (destinationFolder.exists()) {
            try (ZipFile zipFile = new ZipFile(createZipFile())) {
                zipFile.addFile(new File(backupFolder(), fileName.getName()));
                return zipFile.getFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return destinationFolder;
    }

    private File unZipFile(String zippedFile) {
        try (ZipFile theZipFile = new ZipFile(zippedFile)) {
            List<FileHeader> fileHeaders = theZipFile.getFileHeaders();
            File jsonFile = createFile(fileHeaders.get(0).getFileName(), null);
            theZipFile.extractFile(jsonFile.getName(), backupFolder());
            return jsonFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createZipFileName() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        return "local_file" + "_" + dtf.format(now) + ZIP_TYPE;
    }

    private String createFileName() {
        return UUID.randomUUID().toString();
    }

    public List<String> listFiles() {
        File theFolder = new File(backupFolder());
        return Arrays.stream(Objects.requireNonNull((theFolder).listFiles((dir, name) -> name.endsWith(ZIP_TYPE)), "the folder does not exist")).map(File::getName).sorted(Collections.reverseOrder()).collect(Collectors.toList());
    }

    private File ensureFolderExists() {
        File destinationFolder = new File(backupFolder());
        try {
            if (destinationFolder.exists() || destinationFolder.mkdirs()) {
                return destinationFolder;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return destinationFolder;
    }

    private File createZipFile() {
        File theZipFile = new File(ensureFolderExists(), createZipFileName());
        try {
            if (!theZipFile.exists() || theZipFile.createNewFile()) {
                return theZipFile;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return theZipFile;
    }

    private File createFile(String fileName, String fileType) {
        File backupFolder = ensureFolderExists();
        File backupFile = new File(backupFolder, fileType == null ? fileName : fileName + fileType);
        try {
            if (!backupFile.exists()) {
                boolean b = backupFile.createNewFile();
            } else if (backupFile.exists()) {
                FileUtils.write(backupFile, "", Charset.defaultCharset());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return backupFile;
    }

    private void deleteJsonFile(String fileName) {
        File file1 = new File(backupFolder(), fileName);
        try {
            boolean b = file1.delete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkFileExists(String fileName) {
        File theFile = new File(backupFolder(), fileName);
        if (!theFile.exists()) {
            throw new RuntimeException("'" + fileName + "' does not exist");
        }
    }

    public void saves(MultipartFile file) {
        ensureFolderExists();
        File tempFile = new File(backupFolder(), Objects.requireNonNull(file.getOriginalFilename()));
        if (tempFile.exists()) {
            throw new RuntimeException("the file '" + file.getOriginalFilename() + "' already exists");
        }
        try (OutputStream os = new FileOutputStream(tempFile)) {
            os.write(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loads(HttpServletResponse response, String fileName) {
        checkFileExists(fileNameDotZip.apply(fileName));
        try {
            File file = new File(backupFolder() + fileNameDotZip.apply(fileName));
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) {
                mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
            response.setContentLength((int) file.length());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void remove(String fileName) {
        checkFileExists(fileNameDotZip.apply(fileName));
        File theFile = new File(backupFolder() + fileNameDotZip.apply(fileName));
        boolean b = theFile.delete();
    }

    public String backup(UserAddressDTO userAddressDTO) {
        String fileName = createFileName();
        String JSON_TYPE = ".json";
        File destinationFile = createFile(fileName, JSON_TYPE);
        try (FileWriter fw = new FileWriter(destinationFile); BufferedWriter bw = new BufferedWriter(fw)) {
            Gson gson = new Gson();
            bw.write(gson.toJson(userAddressDTO));
            bw.flush();
            return zipFile(destinationFile).getName();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteJsonFile(destinationFile.getName());
        }
    }

    public UserAddressDTO restore(String fileName) {
        checkFileExists(fileNameDotZip.apply(fileName));

        File extractedFile = unZipFile(backupFolder() + fileNameDotZip.apply(fileName));

        try (BufferedReader reader = new BufferedReader(new FileReader(extractedFile))) {
            return new Gson().fromJson(reader, UserAddressDTO.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            boolean b = extractedFile.delete();
        }
    }

}
