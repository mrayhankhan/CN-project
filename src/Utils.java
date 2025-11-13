/**
 * Utils - Utility functions for JSON encoding and HTML escaping
 */
public class Utils {
    
    /**
     * Escape HTML to prevent XSS attacks
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Convert string to JSON string format (with quotes and escaping)
     */
    public static String toJsonString(String text) {
        if (text == null) return "\"\"";
        
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        
        sb.append('"');
        return sb.toString();
    }
    
    /**
     * Unescape JSON string
     */
    public static String unescapeJson(String text) {
        if (text == null) return "";
        
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (escaped) {
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        // Unicode escape
                        if (i + 4 < text.length()) {
                            String hex = text.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Parse URL-encoded form data and extract the 'text' parameter
     */
    public static String parseFormData(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        
        // Look for text= parameter
        String[] params = body.split("&");
        for (String param : params) {
            if (param.startsWith("text=")) {
                String encoded = param.substring(5); // Remove "text="
                return urlDecode(encoded);
            }
        }
        
        return "";
    }
    
    /**
     * URL decode a string (decode %XX and + to space)
     */
    public static String urlDecode(String encoded) {
        if (encoded == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '+') {
                sb.append(' ');
            } else if (c == '%' && i + 2 < encoded.length()) {
                try {
                    String hex = encoded.substring(i + 1, i + 3);
                    int value = Integer.parseInt(hex, 16);
                    sb.append((char) value);
                    i += 2;
                } catch (NumberFormatException e) {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
