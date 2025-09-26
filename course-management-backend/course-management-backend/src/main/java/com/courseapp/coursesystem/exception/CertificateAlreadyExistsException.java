package com.courseapp.coursesystem.exception;

public class CertificateAlreadyExistsException extends RuntimeException {
  public CertificateAlreadyExistsException(String message) {
    super(message);
  }
}
