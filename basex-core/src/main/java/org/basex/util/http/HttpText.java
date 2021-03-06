package org.basex.util.http;

import static org.basex.query.QueryText.*;
import static org.basex.util.Token.*;

import java.util.*;

import org.basex.query.value.item.*;

/**
 * HTTP strings.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Rositsa Shadura
 */
public interface HttpText {
  /** HTTP header: WWW-Authentication string. */
  String WWW_AUTHENTICATE = "WWW-Authenticate";
  /** HTTP header: Authorization. */
  String AUTHORIZATION = "Authorization";
  /** HTTP header: Content-Type. */
  String CONTENT_TYPE = "Content-Type";
  /** HTTP header: Content-Transfer-Encoding. */
  String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  /** HTTP header: Location. */
  String LOCATION = "Location";
  /** HTTP header: Accept. */
  String ACCEPT = "Accept";

  /** HTTP basic authentication. */
  String BASIC = "Basic";
  /** HTTP digest authentication. */
  String DIGEST = "Digest";

  /** Content-Disposition. */
  byte[] CONTENT_DISPOSITION = token("Content-Disposition");
  /** Dashes. */
  byte[] DASHES = token("--");

  /** Character string. */
  String CHARSET = "charset";
  /** Filename string. */
  String FILENAME = "filename";

  /** Default multipart boundary. */
  String DEFAULT_BOUNDARY = "1BEF0A57BE110FD467A";
  /** Boundary marker. */
  String BOUNDARY = "boundary";
  /** HTTP method TRACE. */
  String TRACE = "TRACE";
  /** HTTP method DELETE. */
  String DELETE = "DELETE";
  /** Body attribute: src. */
  String SRC = "src";

  /** MD5. */
  String MD5 = "MD5";
  /** MD5-sess. */
  String MD5_SESS = MD5 + "-sess";
  /** Auth. */
  String AUTH = "auth";
  /** Auth-int. */
  String AUTH_INT = "auth-int";

  /** QName. */
  QNm Q_BODY = new QNm(HTTP_PREFIX, "body", HTTP_URI);
  /** QName. */
  QNm Q_RESPONSE = new QNm(HTTP_PREFIX, "response", HTTP_URI);
  /** QName. */
  QNm Q_HEADER = new QNm(HTTP_PREFIX, "header", HTTP_URI);
  /** QName. */
  QNm Q_MULTIPART = new QNm(HTTP_PREFIX, "multipart", HTTP_URI);

  /** Carriage return/line feed. */
  byte[] CRLF = { '\r', '\n' };

  /** Response attribute: status. */
  byte[] STATUS = token("status");
  /** Response attribute: message. */
  byte[] MESSAGE = token("message");

  /** Header attribute: name. */
  String NAME = "name";
  /** Header attribute: value. */
  String VALUE = "value";

  /** Binary string. */
  String BINARY = "binary";
  /** Base64 string. */
  String BASE64 = "base64";

  /** Request attributes. */
  enum Request {
    /** NC. */ NC,
    /** QOP. */ QOP,
    /** URI. */ URI,
    /** Href. */ HREF,
    /** Nonce. */ NONCE,
    /** Realm. */ REALM,
    /** Opaque. */ OPAQUE,
    /** Cnonce. */ CNONCE,
    /** Method. */ METHOD,
    /** Timeout. */ TIMEOUT,
    /** Response. */ RESPONSE,
    /** Password. */ PASSWORD,
    /** Username. */ USERNAME,
    /** Algorithm. */ ALGORITHM,
    /** Auth-method. */ AUTH_METHOD,
    /** Status-only. */ STATUS_ONLY,
    /** Follow-redirect. */ FOLLOW_REDIRECT,
    /** Send-authorization. */ SEND_AUTHORIZATION,
    /** Override-media-type. */ OVERRIDE_MEDIA_TYPE;

    /** Cached enums (faster). */
    public static final Request[] VALUES = values();

    /**
     * Returns an enum for the specified string.
     * @param key key
     * @return enum
     */
    public static Request get(final String key) {
      for(final Request r : VALUES) {
        if(key.equals(r.toString())) return r;
      }
      return null;
    }

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
    }
  }
}
