/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.sdk.dto;

import com.opentext.ia.sdk.support.JavaBean;


public class Sip extends JavaBean {

  private String format;
  private String extractorImpl;

  public Sip() {
    setFormat("sip_zip");
    setExtractorImpl("com.emc.ia.reception.sip.extractor.impl.ZipSipExtractor");
  }

  public String getFormat() {
    return format;
  }

  public final void setFormat(String format) {
    this.format = format;
  }

  public String getExtractorImpl() {
    return extractorImpl;
  }

  public final void setExtractorImpl(String extractorImpl) {
    this.extractorImpl = extractorImpl;
  }

}
