package com.io.file.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileInformationDTO implements Serializable {


  private String description;
  private String fileName;
  private Integer formatModelCount = 0;
  private Integer ownerCount = 0;
  private Integer formatConversionCount = 0;
  private Integer formatConversionAuditCount = 0;
}
