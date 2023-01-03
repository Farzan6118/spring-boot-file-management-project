package com.io.file.controller;

import com.io.file.model.UserAddressDTO;
import com.io.file.service.BackupRestoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/file")
public class BackupRestoreController {
    private final Logger log = LoggerFactory.getLogger(BackupRestoreController.class);
    private final BackupRestoreService backupRestoreService;

    @GetMapping("/file/download/{fileName}")
    public void fileDownload(@PathVariable String fileName, HttpServletResponse response) {
        log.info("file download");
        backupRestoreService.loads(response, fileName);
    }

    @PostMapping("/file/backup/{userAddressDTO}")
    public ResponseEntity<String> backupRequest(@PathVariable UserAddressDTO userAddressDTO) throws URISyntaxException {
        log.info("backup models called");
        String userAddressDTOres = backupRestoreService.backup(userAddressDTO);
        return ResponseEntity.created(new URI("/file/backup")).body(userAddressDTOres);
    }

    @PostMapping("/file/restore/{fileName}")
    public ResponseEntity<UserAddressDTO> restoreRequest(@PathVariable String fileName) throws URISyntaxException {
        log.info("restore '" + fileName + "'");
        UserAddressDTO res = backupRestoreService.restore(fileName);
        return ResponseEntity.created(new URI("/api/file/restore/" + fileName)).body(res);
    }

    @PostMapping(value = "/file/upload")
    public ResponseEntity<Void> fileUpload(@RequestParam("file") MultipartFile file) throws URISyntaxException {
        log.info("file upload");
        backupRestoreService.saves(file);
        return ResponseEntity.created(new URI("/api/file/upload")).build();
    }

    @GetMapping("/file/list")
    public ResponseEntity<List<String>> listBackupFiles() throws URISyntaxException {
        log.info("list fileNames");
        List<String> res = backupRestoreService.listFiles();
        return ResponseEntity.created(new URI("/api/file/list")).body(res);
    }

    @DeleteMapping("/file/delete/{fileName}")
    public ResponseEntity<Void> removeFile(@PathVariable String fileName) throws URISyntaxException {
        log.info("remove from");
        backupRestoreService.remove(fileName);
        return ResponseEntity.created(new URI("/api/file/delete/" + fileName)).build();
    }
}
