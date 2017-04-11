package uk.ac.warwick.tabula.system.exceptions

import org.springframework.web.multipart.MultipartException

class FileUploadException(mpe: MultipartException) extends RuntimeException(mpe) with HandledException