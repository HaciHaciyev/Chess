package core.project.chess.domain.user.util;

import core.project.chess.domain.commons.containers.StatusPair;
import io.quarkus.logging.Log;
import org.apache.tika.Tika;

public class PictureUtility {

    private PictureUtility() {}

    private static final Tika tika = new Tika();

    public static final byte[] DEFAULT_IMAGE = {
            -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 1, 44, 0, 0, 0, -40, 8, 2, 0, 0, 0, -34, 107, 73, -103, 0, 0, 28, 41, 73, 68, 65, 84, 120, 94, -19, -99, -19, 118, -29, -42, -83, 64, -25, 9, 111, -101, -81, 73, -101, 52, -9, -33, 109, -101, 76, 102, -14, -47, 62, 74, -37, -52, -40, -78, -100, -66, -23, -107, 13, 9, -122, -79, 121, -64, 67, -14, -112, 34, 41, -84, -75, -41, 68, -108, 14, 113, 112, 0, 108, -113, 109, 121, -100, 55, -65, -3, -3, 75, -31, -29, -9, 95, -99, -8, -12, -61, 91, 65, 46, -11, -103, -69, 119, 95, 11, -70, -64, -15, -97, -65, 125, 49, 14, -105, -64, -57, 31, -66, -24, -28, -18, -57, -81, 58, -7, -12, -18, -53, 78, 24, 33, -26, -73, -17, 63, -17, -30, -77, -1, -4, -3, -113, -1, -7, -5, -1, 60, -1, -87, 124, -10, 66, -7, 56, -107, -4, -5, -81, -97, 15, -126, 59, 10, -38, 47, 7, 59, -43, -117, -10, 90, -38, 125, 10, -14, -101, 118, 7, -16, 68, 2, 87, 62, -15, 84, -25, -49, 126, -5, -2, 15, -49, 127, 42, 79, -91, 62, 55, -126, -73, 20, -32, -114, -29, 56, 53, -15, -33, 127, -5, -29, -119, 127, -3, -11, 15, 39, -28, -15, -23, 73, 76, -62, -103, 127, -3, -33, 103, -99, -80, 35, 2, 59, 40, -40, 28, -34, -56, 127, -12, 108, -38, 9, 119, 57, -109, -124, -102, -57, 75, 125, -95, -121, 64, -3, 82, 66, 11, -57, -44, 117, -80, -98, 89, 37, -4, -8, -61, -55, -73, 63, 62, -1, -87, -104, 70, -16, -106, 2, -36, 113, 36, -49, -115, -74, 61, -59, 12, -68, -126, 29, -119, -5, 82, -62, -26, -16, 36, -95, 30, -52, 118, -62, 94, -70, -82, 116, -30, -49, 86, -115, -33, 29, 58, 9, -44, -81, -83, -124, 5, -98, 60, -20, -4, -56, 125, -90, 124, -100, 74, -40, -98, 113, 48, -78, -64, 78, -11, 50, -85, -124, -97, -34, -99, -8, -20, -7, 79, -59, -12, -117, -73, 20, -32, -114, 49, -116, 112, -26, 121, 95, -37, 83, 62, 99, 97, -27, 5, -18, 88, -49, 27, -51, -58, -11, -128, 45, -71, -1, -15, 79, 39, 108, -121, 44, -2, 108, 67, 120, -75, 23, 52, 19, -18, -33, -65, -19, -124, 43, 99, 57, 7, -14, -28, -31, -96, -113, -36, -100, -23, 24, -74, 100, 28, -116, 44, -80, 83, 49, -18, -106, 79, 125, 18, 14, -93, 44, -31, 25, -28, -17, 35, -116, -123, -111, -49, -15, -95, -36, -85, -2, 18, 68, -114, -31, -114, 60, -41, 89, 66, 125, -51, 86, -33, 94, -118, -127, 109, 37, -28, -90, 79, 64, -89, -108, -80, 6, 70, 22, -40, -87, 24, 119, -53, -89, -42, 18, -106, 62, -77, 56, 23, 28, -7, -5, 8, 99, 97, 100, -31, -4, -9, 27, 62, 29, -11, -18, 93, 96, -124, 56, 79, 86, 88, -80, 107, -34, -40, 64, -70, -62, -55, -90, 6, 30, -34, -1, 89, 31, 59, -104, 86, 13, -36, -126, -102, 9, -121, 15, 95, 119, -62, -107, -79, -100, 3, 121, -14, 112, -48, 71, 110, -106, 59, -122, 109, 27, 7, 35, 11, -20, 84, -116, 59, -62, -89, 62, 9, 89, 1, -127, 43, -97, 40, 75, 120, 46, 56, -14, -9, 17, -6, -10, 45, -63, -56, 66, 73, 66, 124, 56, 46, 118, 60, -122, 59, 10, 118, -51, -117, -124, -6, -78, 52, -61, 93, -34, 63, 27, 24, 72, -56, 109, 42, 113, -15, -87, 89, 12, -11, 27, 39, 33, 35, 60, 115, 122, -23, -12, -27, -24, -25, -49, 127, 42, -26, -58, -14, 113, 42, 97, -37, 98, 56, -114, 2, 51, 25, -99, -113, -67, 93, 118, -116, 36, -28, -104, 62, -61, -65, 67, 46, 116, 127, 102, 81, -86, 39, 119, 60, -17, -117, -54, -60, -16, -92, -126, -20, -50, 79, 71, 121, -94, -8, 92, -100, 40, -127, -103, 16, 47, -95, 38, -25, -70, 120, 54, -92, -75, -124, 122, -69, -58, -89, 102, 49, -48, 38, 37, -100, -102, -113, -67, 93, 118, 108, 39, -31, -25, -91, -49, 44, 74, -11, -28, -114, -25, 125, 81, -103, 24, -98, 84, -72, -69, 124, -27, 34, 25, 106, 50, 60, 81, 120, -82, 105, 18, -70, 83, -79, 10, -110, 107, -81, -124, 37, 116, 39, 27, -19, -34, 90, -25, -128, 102, -29, -128, 78, -29, -8, -22, -103, 47, 46, 15, -124, -89, -105, -50, 93, 65, 65, 89, -127, 24, 70, -120, 97, -125, 98, -72, 99, 37, 114, 123, -1, -66, 24, -69, 16, -7, 40, 54, -32, -125, -38, -20, -8, 12, -49, 80, 63, -127, 43, 123, -32, -114, 96, 33, 9, 93, -76, -5, -108, -16, 2, 35, -60, -80, 65, 49, -36, -79, 18, -71, -67, 127, 95, -114, 93, -124, 24, -104, 18, -66, 98, 9, 9, 93, 40, -63, -69, -105, 18, -42, -63, 6, -59, 112, -57, 74, -28, -10, -2, 125, -97, 70, -51, 26, -43, -53, -42, 37, 28, 68, -43, -119, 102, -105, -48, -59, -71, 15, -12, -37, -108, -124, -91, -95, 97, 5, 98, 24, -95, 45, -36, -79, 18, -71, -67, 71, -62, -13, -56, 114, -8, 2, -74, 33, 97, 25, -98, 40, -124, 59, -126, -123, 36, -44, 75, -81, 28, -127, 78, -29, -128, 78, -29, 72, 9, 83, 66, 7, 79, 20, -62, 29, -63, -20, 18, 90, 52, -56, -61, -121, 111, -68, 123, 91, -109, -16, 12, -50, 56, 55, 108, 80, 12, 35, 84, 34, -73, 87, 72, -120, -79, -21, 97, 27, 18, 98, 18, -50, -32, 56, -67, -12, 31, 106, 57, 9, -83, -127, 41, -31, 104, -40, -96, 24, 70, -88, 68, 110, -81, -109, 80, -68, 26, -60, -21, 49, -35, -104, -124, 60, 78, -119, 85, 74, 40, 6, 110, 71, -62, -73, 69, -3, 4, 28, 115, 110, -40, -96, 24, 70, -88, 68, 110, -97, 65, -62, -126, 126, 91, -110, 112, 16, -3, -121, 122, -11, 99, 107, 22, 118, 101, 28, -34, -79, -91, 96, 38, -29, -14, -71, -82, 114, -53, 115, -9, -66, -101, -113, -17, -34, 118, -15, -27, -57, 31, -65, -8, -8, -29, 103, -28, -18, -61, 23, -28, -2, -23, -121, 31, -66, -70, 127, -89, 124, -83, 28, 78, -83, 1, 113, 74, 77, 96, 5, -50, -101, 66, 21, -127, -33, 47, -115, 97, 4, -110, 18, -10, -64, 8, -5, -122, 99, 42, 64, -65, 49, 18, -34, 125, -16, -18, 109, 79, 66, -104, 18, -61, 8, 36, 37, -20, -127, 17, -10, 13, -57, 84, -128, 126, 86, -62, 14, 78, -66, 117, -110, 18, -110, -35, 74, -40, 10, -98, 104, -33, 112, 76, -123, 79, 63, 126, -35, -59, 87, 79, -68, -1, -110, 80, -65, -117, -124, 94, -65, 109, 73, 56, 7, 41, 97, 15, 60, -47, -66, -31, -104, 10, -48, 47, 37, 108, -58, 110, 37, -44, 111, -61, 58, -72, 50, -122, 39, -38, 55, 28, -45, 62, -34, -46, -76, 34, -17, -97, 62, 61, -93, -127, -127, -124, -40, -82, 49, -84, -64, -71, 14, 80, 69, -32, -54, 24, 70, 32, 41, 97, 15, 60, -47, -66, -31, -104, -10, -15, 118, 24, 24, 65, -127, -103, -116, -51, 103, 24, -36, -15, -68, 47, 50, -116, -13, 44, -63, 8, 36, 37, -20, -127, 39, -38, 55, 28, -45, 62, -96, 89, 12, 70, 80, 96, 38, 99, -13, 25, 6, 119, 60, -17, -117, 12, -29, 60, 75, 48, 2, 121, -61, -89, 4, -122, -117, -31, -8, -74, -123, 58, -75, -123, 59, -74, -123, 21, 22, -72, 114, 97, -92, 125, 47, 41, 97, 76, -5, -128, 102, 61, 48, -62, 53, -31, 36, 47, 79, 74, 120, -122, 59, -74, -123, 21, 22, -72, 114, 97, -92, 125, 47, 41, 97, 76, -5, -96, 102, 49, -116, 112, 77, 56, -55, -53, -109, 18, -98, -31, -114, 109, 97, -123, 5, -82, 92, 24, 105, -33, 75, 74, 24, -45, 125, -61, 73, 94, -98, -108, -16, 12, 119, 108, 11, 43, -74, 76, -35, 122, -111, 52, 82, -62, 43, -110, 18, -98, -31, -114, 109, 97, -59, -106, -87, 91, 47, -110, -58, 98, 18, 126, -4, 48, 12, 70, 104, 11, 59, -78, 60, 41, -31, 25, -18, 120, 35, 72, -5, 82, -62, 43, -110, 18, -98, -31, -114, 55, -126, -76, 47, 37, -68, 34, 41, -31, 25, -18, 120, 35, 72, -5, 82, -62, 43, 82, -108, -48, -39, -8, -46, -77, 15, -35, 28, 126, -6, 102, 19, -80, 13, -25, 102, -32, 68, 55, -126, 28, 31, 63, 20, -22, 97, -59, -42, 9, 71, 124, 28, 20, -63, -22, -48, -106, -108, -16, -46, 60, -100, -24, 70, -112, -29, -45, -70, -108, -80, 19, -82, -100, 78, 74, 120, 105, 30, 78, 116, 35, -56, -15, 105, 93, 74, -40, 9, 87, 78, -25, -26, 36, 100, -26, 55, -114, -52, 46, -83, 75, 9, 59, -31, -54, -23, -92, -124, -73, -114, -52, 46, -83, 75, 9, 59, -31, -54, -23, -36, -100, -124, 37, 120, -94, 27, 65, 102, -105, -42, -91, -124, -99, 112, -27, 116, 82, -62, 51, 60, -47, -115, 32, -77, 75, -21, 82, -62, 78, -72, 114, 58, 41, -31, 25, -98, -24, 70, -112, -39, -91, 117, 41, 97, 39, 92, 57, -99, -2, 127, -44, 43, -18, -23, -101, -38, -20, -94, -64, -79, 94, 39, -52, 60, -79, 80, -65, 24, 106, 48, 14, 70, 22, -104, 97, -116, -2, 109, 81, 66, -90, -102, 82, -55, -91, -50, 63, -11, -117, -31, 70, 118, 59, 98, -41, -92, -124, -55, 43, -88, 65, 12, 117, 26, 7, 35, 11, -52, 48, -122, 26, 56, 82, -62, -21, -61, -52, 19, 11, 53, -120, -95, 78, -29, 96, 100, -127, 25, -58, 80, 3, -57, 26, 37, 100, 56, 33, 37, -68, 77, -88, -57, 117, 97, -122, 49, -44, -64, 49, -109, -124, -44, -84, -98, -108, 48, 121, 5, 53, -72, 46, -52, 48, -122, -42, 109, 64, 66, 62, -27, 72, 9, -109, 0, 106, 51, 14, 70, 30, 7, -83, 91, 70, -62, 18, 20, -54, 37, 112, 34, 37, 76, 38, 65, -99, -58, -63, -56, -29, -96, 117, 59, -108, -16, -23, 24, 56, -7, -45, -31, 49, -18, -21, -124, -103, 39, 83, -96, 78, -29, 96, -28, 113, -48, -70, 13, 72, 120, 119, -38, -17, 25, -9, -37, -56, 31, 78, -30, 1, 30, 73, 81, 75, 29, -44, 96, 91, 60, -4, -4, -83, 32, -105, -20, 122, -78, 45, -60, 121, 126, 119, 87, 46, -11, 127, 56, -59, -17, -45, -58, -72, 8, -6, 36, -1, -9, -93, 100, -104, -124, -127, -121, -44, 47, 37, 76, 86, 72, 74, -72, 61, 82, -62, -99, -111, 18, 110, -113, -108, 112, 103, -92, -124, -37, 35, 37, -36, 25, 41, -31, -10, 72, 9, 119, 70, 74, -72, 61, 82, -62, -99, -111, 18, 110, -113, -108, 112, 103, -84, 81, 66, -75, -50, 65, -3, -50, 92, 126, 79, 110, -25, 91, -7, -124, 99, -67, 81, 108, 11, -17, -38, -67, -71, 60, 55, -102, -80, -125, 7, -36, -57, 121, 75, -12, -98, 75, 46, 105, 87, 37, 122, 111, 74, 56, 35, -91, -26, -83, 28, 77, -40, -63, 3, -18, -29, -68, 37, 122, -49, 37, -105, -76, -85, 18, -67, 55, 37, -100, -111, 82, -13, 86, -114, 38, -20, -32, 1, -9, 113, -34, 18, -67, -25, -110, 75, -38, 85, -119, -34, -101, 18, -50, 72, -87, 121, 27, -123, 7, -68, -111, -13, -54, 37, -49, -107, 18, 110, -128, 82, -13, 54, 10, 15, 120, 35, -25, -107, 75, -98, 43, 37, -36, 0, -91, -26, -83, 28, 30, -92, 18, -71, 125, 115, -25, 45, -47, 123, -82, -108, 112, 3, -108, -102, -73, 114, 120, -112, 74, -28, -10, -51, -99, -73, 68, -17, -71, 54, 35, -95, -8, -106, 18, 110, 104, 40, 121, -112, 74, -28, -10, -51, -99, -73, 68, -17, -71, -82, 41, -31, -15, -35, -97, 28, 86, 66, 113, -84, 6, -22, -105, 18, 78, -127, 9, 44, 3, 51, -39, 55, -38, -48, -103, -72, 127, -1, -74, -105, 1, 18, 74, -46, -44, 47, -122, -57, 62, -57, 65, -5, 87, -114, -21, 25, 79, -44, 22, 38, -80, 12, -52, 100, -33, 80, -101, -74, 80, 57, -110, 18, -42, -30, 122, -58, 19, -75, -123, 9, 44, 3, 51, -39, 55, -44, -90, 45, 84, -114, 52, -109, 80, -66, 68, 36, 76, 75, 96, -5, 87, -114, -21, 25, -37, -39, 22, 38, -80, 12, -52, 100, -33, 112, 50, -37, 66, -27, 72, 74, 88, -117, -21, 25, -37, -39, 22, 38, -80, 12, -52, 100, -33, 112, 50, -37, 66, -27, 72, 51, 9, 75, -16, -40, -25, 56, 104, -1, -54, 113, 61, -29, -119, -38, -62, 4, -106, -127, -103, -20, 27, 106, -45, 22, 42, 71, 82, -62, 90, 92, -49, 120, -94, -74, 48, -127, 101, 96, 38, -5, -122, -38, -76, -123, -54, -111, 102, 18, -14, -51, -119, 124, -117, 98, 10, 76, 96, 25, -104, -55, -66, -95, 54, 109, -95, 114, 36, 37, 28, 9, -37, -39, 22, -18, -72, 12, -52, -28, -70, -24, -65, -88, -106, -53, -69, -53, 123, -30, 92, 57, 17, 113, -58, -66, -7, -34, 6, -68, 53, 47, 28, 62, 124, 45, -76, -108, -80, 8, -38, -68, 15, -40, -59, -74, 112, -57, 101, 96, 38, -41, 101, -33, 18, 54, -2, -101, -80, 8, -38, -68, 15, -40, -59, -74, 112, -57, 101, 96, 38, -41, 101, -57, 18, -74, -1, 116, -108, 111, 78, 8, 108, -13, 62, 96, 23, -37, -62, 29, -105, -127, -103, 92, 23, -105, -104, 14, 55, 87, 78, 68, -62, 46, 38, -95, 26, 120, 55, -24, 103, 71, 37, 87, -22, -105, 18, -50, 1, 119, 92, 6, 102, 114, 93, -104, -107, 12, 55, 87, 78, 68, -62, 46, 44, -95, 60, 110, 38, 97, 17, -76, 121, 31, -80, -117, 109, -31, -114, -53, -64, 76, 110, 4, 113, 102, 49, 9, 45, 41, -31, 72, -40, -59, -74, 112, -57, 101, 96, 38, 55, -126, 56, -77, -68, -124, -97, -34, 125, -7, 36, -31, 73, 57, -5, -25, 56, 9, -7, -26, 68, -66, 69, 49, 5, -18, -72, 12, -52, -28, 70, 16, 103, -82, 35, -31, 65, -66, 114, 51, 127, -98, -65, -106, -125, 102, 86, 69, -62, 118, 38, -101, -64, -114, -32, -35, -16, -81, -75, -4, -52, 93, -32, 70, 49, -116, -68, 48, -110, -74, 74, 88, -126, -1, 27, -48, -23, 20, -1, 79, -67, -44, 47, 37, -36, 37, 118, 4, 83, 66, 90, -105, 18, 38, -77, 99, 71, 48, 37, -92, 117, 41, 97, 50, 59, 118, 4, 71, 72, 88, -126, 27, -59, 48, -62, -62, -92, -124, -55, -43, -80, 35, -104, 18, -46, -70, -108, 48, -103, 29, 59, -126, 35, 36, 100, -64, 113, 48, -14, -62, -92, -124, -55, -43, -80, 35, -104, 18, -46, -70, -108, 48, -103, 29, 59, -126, 41, 33, -83, 91, -93, -124, 44, 95, 114, 11, -88, -91, 14, -82, 20, -12, 127, -82, 90, 66, -106, -87, 3, -116, -32, 22, 44, 12, 79, 122, 6, 10, -59, -48, 44, -63, -82, 73, 9, -109, 42, -4, 44, -90, -124, 117, -48, -84, -108, 48, 25, -119, -97, -59, -108, -80, 14, -102, -107, 18, 38, 35, -31, -104, 10, 92, 41, -48, -70, -108, 48, 37, 76, 38, -63, 49, 21, -72, 82, -96, 117, 41, 97, 74, -104, 44, 10, -83, 75, 9, 83, -62, 100, 81, 104, 93, 74, -104, 18, 38, -117, 66, -21, 82, -62, -94, -124, -97, 126, 120, 43, -72, 123, -68, 123, 41, 97, 50, 4, 25, -27, -61, -59, -58, -29, 47, 127, 121, -4, -11, -69, 19, 92, -39, -117, -75, 66, -61, -50, -115, 119, -17, 2, 117, 21, -8, -74, -2, -7, -51, 125, 68, 16, -20, -102, -108, 48, -103, -123, -108, 48, 37, 76, -82, 76, 74, -104, 18, 110, 6, 126, -79, 100, -65, 100, -38, 1, 42, -95, -64, 5, -67, 92, 69, 66, 106, 22, 67, -51, 98, 82, -62, 21, 65, -3, -10, 36, -95, 61, 81, 74, 104, 73, 9, 87, -124, -2, 21, -31, -32, -54, 109, -63, 15, 43, -93, 63, -72, -40, 41, -105, -57, 92, -77, 24, -44, 47, -122, -6, -91, -124, -85, -125, -6, -91, -124, 14, 59, -51, -14, -104, 107, 22, -125, -102, -59, 80, -65, -108, 112, 117, 80, -65, -108, -48, 97, -89, 89, 30, 115, -51, 98, 80, -77, 24, -22, -41, 33, -95, -72, -89, -6, -87, 108, -50, -58, -35, -1, 50, -33, 107, 65, -3, -124, -57, 95, -65, 59, -115, -84, 52, 82, 23, -37, -57, 43, 71, -89, 80, 46, -89, 75, -88, -47, 30, -98, -65, -68, -28, -78, -103, 112, 46, -39, 100, -90, -112, 18, -82, 8, -22, -105, 18, 58, -20, -20, 30, 82, -62, -108, -80, 57, -44, 47, 37, 116, -40, -39, 61, -92, -124, 41, 97, 115, -88, 95, 74, -24, -80, -77, 123, -72, 72, -88, 1, 29, -68, 125, 34, -78, 111, 74, -72, 103, -88, 95, 74, -24, -80, -77, 123, 72, 9, 83, -62, -26, 80, -65, -108, -48, 97, 103, -9, 112, -111, -80, 4, 111, -97, -120, -20, -101, 18, -18, 25, -114, -111, -112, 18, 42, 118, 118, 15, 41, 97, 74, -40, 28, -114, -111, -112, 18, 42, 118, 118, 15, -69, -108, -16, -2, -92, -33, 51, -10, -1, 13, 90, -30, 73, 72, 100, 25, -61, 50, -115, -125, -111, -109, 26, -76, -21, 114, 57, 69, -122, -85, -32, 82, 21, 3, 79, 31, -95, 56, 33, 51, -51, -119, 84, -81, -71, -124, 18, 89, 98, 14, -106, 112, -24, 95, -122, 44, 83, -116, 78, -119, -125, -111, -109, 26, 108, -41, 15, 41, -31, 112, -92, 122, -51, 37, 20, -18, -98, 127, 116, 102, 51, 18, -14, 0, 2, 119, 76, 44, -82, 80, 55, 43, 33, 39, -89, 114, 126, 100, 89, 115, 9, 37, -38, -55, -64, -113, -17, -34, 54, -109, -112, -38, 8, 44, 83, 12, 35, -37, 90, 16, -82, 76, 44, -82, 80, -69, -105, -112, 19, 40, 112, 114, 42, -25, 71, -106, -51, 33, -95, 24, -8, -37, 15, 95, -91, -124, 59, -57, 21, 74, -5, -62, -107, -21, -60, -91, 42, 19, -107, 18, -6, 44, 5, -106, 99, 28, 44, -97, -64, 29, -109, 26, -76, -21, 114, -71, -71, 122, -70, 84, 31, -6, 36, 44, -63, -56, -107, -88, 51, -51, 37, 108, -1, -23, 40, -113, 61, 14, -22, 23, -61, 76, 18, -117, 118, 93, 46, 55, 87, 55, -105, -22, -61, 88, 9, 57, 57, -107, 117, -80, -50, 52, -108, 80, 16, 15, 87, 39, -31, 80, -104, 73, 98, -47, 126, -53, 101, -3, -16, -83, -124, 86, 18, -106, -32, -114, 14, -75, -91, -83, -124, 18, 89, 98, -66, -7, -12, -3, 87, 119, 63, -68, 85, 9, -123, -45, 37, 13, 20, 9, -113, 63, -99, -117, -94, -79, -76, 52, -125, -32, -57, -92, 24, 70, 112, 113, 108, 86, -102, 88, -78, 117, -76, -77, 122, 121, 108, 42, -95, 98, -89, -56, 34, -29, -92, 18, -106, -98, -89, 102, 49, 122, 99, 74, -104, -84, 29, -19, -84, 94, 30, 83, -62, -108, 48, 89, 18, -19, -84, 94, 30, 83, -62, -110, -124, 49, -10, -76, -113, -49, -65, 4, -106, 107, 98, 88, 53, 91, -69, -121, -108, 112, -89, 104, 103, -11, 82, 70, -120, -109, 48, 17, 59, 69, 22, -25, 76, -23, 121, 106, 22, 115, 5, 9, -91, 118, -57, -117, -127, 41, 97, 82, -119, 118, 86, 47, 101, -118, 56, 9, 19, -79, 83, 100, 113, -50, -108, -98, -89, 102, 49, -77, 72, -56, -20, 45, 106, -47, -87, 124, -65, -1, -29, 127, 79, 80, -77, 24, 86, -51, -42, -18, 33, 37, -36, 41, -38, 89, -67, -108, 41, -30, 36, 76, -60, 78, -111, -59, 57, 83, 122, -98, -102, -59, -92, -124, -55, 102, -48, -50, -22, -91, 76, 17, 39, 97, 34, 118, -118, 44, -50, -103, -46, -13, -44, 44, 38, 37, 76, 54, -125, 118, 86, 47, 101, -118, 56, 9, 19, -79, 83, 100, 113, -50, -108, -98, -89, 102, 49, -81, 36, 60, 25, 40, -36, -101, -73, -20, 79, 28, 63, 124, 67, 26, -2, -69, 94, 86, -63, -42, -126, 112, 101, -25, -115, -36, -56, -63, -56, 14, 89, -90, -59, 98, -124, 117, -62, 54, 111, 43, -1, 86, -72, 17, 58, 94, 62, -18, -105, -26, 68, -42, -40, 87, 15, -49, -59, -44, 5, -50, 25, 86, 120, 28, 29, 18, -118, -127, 107, -106, -80, 68, -87, -72, 37, 24, -63, 33, -53, -76, 88, -116, -80, 78, -40, -26, 109, -27, -33, 10, 55, 66, -57, -83, 72, 72, 3, 83, 66, 45, 22, 35, -84, 19, -74, 121, 91, -7, -73, -62, 117, -13, 120, -7, -122, 124, 105, 78, 100, -115, 125, -11, -80, -68, -124, 52, 80, -66, -4, -93, -127, -53, 72, 88, -126, 17, 92, 28, 87, -36, 18, -82, 73, 68, -106, 105, -79, 24, 97, -99, -80, -51, -37, -54, -65, 21, -74, -119, -14, -8, -8, -6, 107, 72, -69, 64, -42, -40, 49, -109, -105, 108, -35, -100, 51, -84, -16, 56, 58, 36, 116, 6, -90, -124, 90, 44, 70, 88, 39, 108, -13, -74, -14, 111, -123, -101, 1, 113, 108, -19, 18, -38, 79, 65, 99, 3, -105, -111, 80, -33, -51, 119, -48, 22, -95, 84, -36, 18, -116, -32, -112, 101, 90, 44, 70, 88, 39, 108, -13, -74, -14, 111, -117, -98, -35, 78, 72, -25, -100, -72, 5, -14, -110, -83, -101, 115, -122, 21, 30, 71, -73, -124, -67, 6, -90, -124, 107, -122, 109, -34, 86, -2, 109, -79, 103, -73, 67, -62, 57, -31, -85, 114, -69, 11, -75, -88, -124, 20, 111, 37, 18, 114, -91, -61, 21, -73, 4, -83, 115, -56, 50, 45, 22, 35, -84, 19, -74, 121, 91, -7, 55, -60, 29, -97, -102, -39, 57, -31, -85, 18, -63, 69, -37, -89, -124, 37, 84, 6, 41, 74, -91, -124, 86, 30, 57, 24, 35, -49, -124, -51, 86, 19, -106, -108, -76, -93, -9, -61, 101, 40, 125, 80, 24, 10, 35, -9, 98, 79, -92, 57, 104, 62, 19, -31, 118, 109, 113, 7, 103, 2, -124, -125, 52, -82, 110, -125, 72, 9, 91, -30, 26, -71, 87, 9, -73, -126, 59, -72, -106, 49, -128, -125, 52, -82, 110, -125, 72, 9, 91, -30, 26, -71, 87, 9, 25, 121, 28, -36, -82, 45, 110, 35, 45, 99, 0, 7, 105, -127, 60, 119, 37, -95, -82, -105, -37, 23, 107, -74, -62, 70, -38, -60, -72, -66, -110, -46, -71, -122, -62, -56, -67, -40, 19, 77, 60, -59, -14, -72, -125, 107, 25, 3, 92, -41, 36, 8, 35, -73, 101, -121, 18, -38, 8, -93, -121, 111, 28, 108, -92, 102, -59, -36, -22, 113, -73, -45, -82, 74, 24, -71, 23, 123, 34, -51, -127, -107, 31, 7, -73, 107, -117, 59, -72, 109, 68, 9, -101, -98, 28, 118, 92, -35, 6, -79, 43, 9, 25, 97, 97, 92, 35, -39, 99, -127, 55, -58, -72, 27, 105, 87, 37, -116, -36, -117, 61, -111, -26, -96, -115, -104, 8, -73, 107, -117, 59, 56, 27, 65, -20, 56, -55, 97, -57, -43, 109, 16, 41, 97, 75, 92, 35, -39, 99, -127, 55, -58, -72, 27, 105, 87, 37, -116, -36, -117, 61, -111, -26, -64, -54, -113, -125, -37, -75, -59, 29, -100, -115, 32, 54, 61, 57, -20, -72, -70, 13, 34, 37, 108, -119, 107, 36, 123, 44, -16, -58, 24, 119, 35, -19, -86, -124, -111, 123, -79, 39, -46, 28, 120, -94, 113, 112, -69, -74, -72, -125, 51, 1, 98, -57, 73, 50, 28, 87, -73, 65, -68, -110, 80, -36, 35, 52, 112, 25, 9, 55, -121, 107, -28, -3, -49, -33, 13, -126, 51, -31, -112, 93, 104, -41, 80, 108, -74, 26, -106, -24, 2, 57, 78, -17, 7, -63, 86, -108, -50, -53, 12, -57, -63, -62, -22, 118, -10, -68, 54, -121, 89, -79, -83, 73, 9, -89, 98, 91, 120, 76, 9, -57, 82, 58, 47, 51, 28, 7, 11, -85, -37, -39, -13, -38, 28, 102, -59, -74, 38, 37, -100, -124, 109, -89, 116, -111, -102, -59, 112, 44, 28, -78, 17, -91, 26, -118, 75, -104, 103, 113, 11, -28, 56, 41, -31, 76, -40, -42, -92, -124, 35, 97, 59, 5, 106, 22, -61, 8, 14, -39, -114, 82, 13, -59, -91, -51, 19, -71, 5, 50, -108, 41, -31, 76, -40, -42, -92, -124, 35, 97, 59, 5, 106, 22, -61, 8, 14, -39, -114, 82, 13, -59, -91, -51, 19, -71, 5, 50, -108, 41, -31, 76, -40, -42, -92, -124, 35, 113, -93, -13, 50, 61, -48, 44, -122, 99, -31, 56, -121, -123, 84, 67, -111, 56, 46, 44, -47, 5, 114, -64, -108, 112, 38, 108, 107, 82, -62, -111, -72, -23, -47, -25, -87, 89, 12, -57, -62, 113, 14, 11, -87, -122, 34, 113, 92, 88, -94, 11, -28, 116, 41, -31, 76, -40, -42, -92, -124, 35, 113, -45, -93, -49, 83, -77, 24, -114, -123, -29, 28, 22, 82, 13, 69, -30, -72, -80, 68, 23, -56, -23, 82, -62, -103, -80, -83, -71, -102, -124, 67, -117, 94, 26, 38, -74, -45, 53, -43, -63, -56, 49, -74, 88, 22, 70, 22, -88, 89, 12, 35, -72, 60, 101, 59, -5, -34, 110, 39, -52, -48, 33, -47, -72, -111, -125, -107, 28, -121, -4, -118, -25, 19, 19, 29, 102, 71, -58, -31, 14, 104, 63, -72, -24, 99, -71, -76, -59, 95, -122, -75, 72, 120, -72, 76, 27, 87, 118, -66, -38, 59, 52, 28, 47, -73, 93, 37, -100, 102, -127, -111, 5, 106, 22, -61, 8, 46, 79, -35, -111, -30, -91, -124, -11, -72, 3, -90, -124, 79, -40, -58, -21, -109, 28, 29, -114, -111, -69, -105, 109, 19, -20, -102, -50, -67, 42, 97, 26, 2, 35, 11, -44, 44, -122, 17, -104, -89, 110, 74, -9, 82, -62, 74, -36, 1, 111, 75, 66, -74, -39, -42, 66, 47, 117, 61, -89, -57, -50, -112, 13, -56, -122, 89, -72, -93, -37, 104, 34, -116, 44, 80, -77, 24, 102, 110, -13, -41, -19, 88, -112, -95, -60, 105, 43, -52, 100, 28, -83, 36, 100, -122, 2, 59, 18, -29, 14, -104, 18, -66, 106, -74, 43, 43, -89, -89, 115, -122, -40, 45, 7, 119, -76, -69, 76, -121, -111, 5, 106, 22, -61, -52, 93, -2, -70, 35, 107, 50, -120, 56, 109, -123, -103, -116, 35, 37, -84, 103, 118, 9, 89, 86, -83, 66, 103, -95, 25, 65, -80, 107, 58, -125, 56, -20, -6, -102, -8, 37, 24, 33, -122, -102, -59, -40, 9, -32, 52, 8, -52, -118, -48, -70, 125, 72, 88, -126, 21, -120, 113, 7, -76, 117, 102, -39, 121, -5, -84, 92, 83, 66, -37, 27, 78, 67, -119, 82, 4, 7, 111, 20, -104, 97, 12, 35, -60, 80, -77, 24, 59, 1, 110, 26, -20, 41, -104, -104, -125, -42, -91, -124, 22, 119, 64, 91, 103, 87, -10, -102, 106, -73, -27, -54, 18, 114, -38, -72, -78, 115, -127, -69, -99, 112, -68, 4, 102, 24, -61, 8, 49, -44, 44, -58, 78, -128, -101, 6, 123, 16, 38, -26, -96, 117, 41, -95, -59, 29, -48, -42, -39, -107, -67, -90, -38, 109, 41, 74, -40, -7, -113, 125, 3, 9, 75, -51, 102, -7, -82, 11, 51, -73, -40, 38, -39, 25, -46, 49, -30, -44, 86, -62, -67, 116, 71, 65, -9, 21, 24, 33, -114, 115, 120, -3, -114, -94, -19, -123, 83, 81, -10, 98, 100, -115, 47, 15, -20, 121, 15, -27, 95, 111, -63, 10, -69, -125, 84, -62, 8, 54, 1, -51, -89, 126, 125, 37, 54, -90, -124, 125, -72, 20, -63, 86, 79, 30, -21, -109, 109, 73, 9, 95, -95, 9, 63, 94, 73, 66, -39, -6, 49, 37, -68, -96, -119, -71, -80, 92, -23, -42, 87, 98, 99, 74, -40, -121, -108, 112, 110, -104, -71, 69, 19, 126, 92, 92, -62, -121, -41, 30, 50, 66, 28, -25, -80, 107, 9, 25, -103, 43, -19, -6, 122, 52, -96, -122, 125, 72, 9, -25, -122, 109, 112, -56, -78, -46, -108, 112, 125, 37, -84, -104, 69, -105, -11, -26, -55, 123, -107, 89, 37, 100, 37, 99, 88, -64, 24, 70, 88, 12, -105, -122, 22, -63, 86, 79, 30, -57, -11, 31, 77, 74, -8, 10, -69, -78, 115, 74, 120, 75, 37, -84, -104, 67, 87, -58, 27, -15, 70, 37, 37, 28, -121, 75, 67, -117, 96, -85, 39, -113, -29, -6, -113, -26, -26, 36, 28, -118, -101, 18, 78, 109, 37, -84, -104, -42, 77, 31, -13, 46, -62, 8, 54, -44, 124, 18, 114, -91, 93, 70, -88, 89, 12, 35, -40, 56, -10, -103, -26, -5, -70, 75, 61, -81, -83, -98, 60, -42, 39, -37, -110, 18, -10, 96, -37, 115, 44, -49, 110, 47, -84, -104, -83, -101, 94, -14, -58, -54, 56, 26, 106, -105, 18, -22, -126, 57, -10, 117, -105, 122, 94, 91, 61, 121, -84, 79, -74, 37, 37, -20, -63, -74, -25, 88, -98, -35, 94, 88, -79, -72, 110, 37, 24, -63, -123, 74, 9, 7, -31, 110, -111, 45, -28, -68, -74, 122, -14, 88, -97, 108, -53, -101, -57, -97, -66, 21, -50, -102, -63, -58, 87, 102, -98, -2, 44, 72, -88, -60, 53, -22, -123, 101, 26, 7, -57, 101, 98, 98, 114, 59, -49, 59, -111, -110, -124, -6, -72, 114, 119, 107, -102, -77, -50, 93, 74, 76, 86, 102, 28, 44, -108, 67, -38, -95, -17, -35, 115, 65, -116, 54, 84, 46, 123, -9, 117, 11, -12, -10, -34, 31, 30, -112, -69, 88, -40, 5, -40, -83, -124, -116, 60, -111, -103, -102, 116, 35, 18, 62, 94, 52, -32, -126, 24, -41, -48, -34, 125, -35, 2, -73, 123, 48, 30, 114, 23, 11, -69, 0, 41, 97, 45, 51, 53, 105, -9, 18, 30, 95, -101, -64, 87, 99, -40, -51, 120, 95, -105, -104, -35, 58, -98, 13, -71, -117, -123, 93, -128, -35, 74, 88, -126, 59, 86, 50, 83, -109, 90, 73, 24, -48, 68, 66, 22, 36, -58, -35, 53, -67, -2, -107, -72, -124, -125, 1, 112, 25, -54, 37, -85, -73, 0, 41, 97, 45, 51, 53, 105, -33, 18, -38, 27, 39, -42, -65, 18, -73, 111, -87, -5, 76, 79, 46, 89, -67, 5, -40, -83, -124, -116, 60, -111, -103, -102, -44, 74, 66, -67, -47, 97, -73, -104, 34, 33, 43, 28, -41, -39, -90, -60, 87, -21, 97, -124, 120, 95, -73, 105, 103, -98, -70, -26, 80, -2, -83, 121, 75, -110, 18, -42, 50, 83, -109, 118, 44, -95, -51, -118, 11, 42, 113, 17, 122, -9, -19, 93, -81, 11, 36, 67, -9, 60, 11, -69, 0, -125, 37, 100, 8, -57, -60, -94, -77, -51, -29, -48, 90, 59, -72, 99, 37, 51, 53, 105, -33, 18, -70, 83, -116, 64, 19, -112, -53, -34, 125, 123, -41, -53, -85, -102, 94, -23, -7, 37, 121, 35, -17, -65, -109, 39, -33, -70, -80, -115, -23, -60, 29, 126, 43, -16, 32, 122, 28, -69, 76, -70, -59, 58, 78, -95, 83, 18, 121, -96, -11, -108, -57, -10, 46, -5, -22, 9, 27, -60, -62, 19, -59, -108, -30, -77, 98, -82, 32, -18, 20, 37, -72, -93, -18, 43, -37, -71, -32, 46, -127, -34, -9, 27, 25, 89, -48, 87, 37, -115, -5, -53, 79, 53, 48, -61, -27, 25, 44, 33, 67, 8, -18, -76, -20, -42, -54, 97, -37, -12, 56, 118, -103, 109, 100, 43, -36, -8, -54, -90, -14, -64, 78, -113, -37, -41, -66, 42, -21, 59, -31, -119, 98, 74, -15, 89, 49, 87, -112, -108, 112, 52, -125, 37, -76, -75, -74, -72, -45, -78, 91, 43, -121, 109, -45, -29, -40, 101, -74, -111, -83, 112, -29, 43, -101, -54, 3, 59, 61, 118, 95, -105, 97, 48, 124, 67, 113, 91, -24, 112, -77, 98, -82, 32, -18, 20, 37, -72, -93, -18, 43, -37, -71, -32, -106, -32, 37, -123, -111, 5, 125, 85, -46, -72, -33, -124, -124, 37, 24, 66, 112, -89, -19, 45, -106, -21, -27, -43, 97, -122, -99, -87, 30, -81, 45, 33, -57, -53, 46, -85, -121, 17, 116, 11, -95, 62, -84, -36, 85, 41, 97, -128, 108, 87, -45, -108, -57, -14, 7, 77, -122, -43, -32, 118, -63, -60, 84, -37, 50, 88, 66, 86, 68, -48, 67, 106, -103, 122, -21, -72, 42, -104, 97, 103, -86, -57, -108, 16, -56, 93, 75, 74, -56, -76, -103, 63, -125, -69, 87, -89, -92, -38, -106, -94, -124, -57, -97, -66, -19, -124, -29, 43, 104, 21, -92, 76, 124, -34, -63, 8, -21, -124, 99, 17, 116, 122, 28, -125, 36, -28, 20, 74, 16, 78, -86, 107, -124, -125, 29, -79, -47, -36, -18, 110, 71, -117, -68, -60, 83, -108, -80, -53, 58, 15, -18, -86, 109, -23, -52, -77, 62, -2, 106, 25, 44, 33, -37, -20, 10, 36, -105, -67, -51, -26, 88, -84, 19, -114, 69, 48, 1, -29, -24, -100, 69, 121, -96, -11, -108, -57, -121, -14, -73, -44, 89, -31, -72, -50, 92, -39, 25, -112, -3, 117, 104, 65, 42, 39, -98, 122, -72, -125, -69, 106, -105, -50, 91, -126, -111, 123, 83, -70, 58, -125, 37, 100, 27, -40, 21, 91, 74, 46, 16, 56, 22, -21, -124, 99, -15, 80, 49, 10, -125, -24, -100, 69, 121, -96, -11, -108, -57, -121, -108, 16, 1, -21, -29, -81, -106, 126, 9, 31, 127, -2, -53, -119, -33, 127, -7, 78, -80, 117, -79, 104, 68, -41, 60, 87, 62, 125, -107, 99, 33, -12, -2, -69, 47, 7, 35, 76, -60, -26, 108, 15, 82, 57, 4, -118, -12, 94, -66, 5, 103, 25, 58, 19, -99, 105, -44, -61, -128, 49, -116, 32, -116, -82, 67, 37, 108, -124, -64, -107, -5, 35, 37, -12, -40, -100, -89, 12, 95, 74, 56, 8, -41, 86, -123, 43, -9, 71, 74, -24, -79, 57, 79, 25, -66, -108, 112, 16, -36, 113, -90, -115, 86, -56, 27, 126, -43, -25, 72, 9, -19, 51, -84, 96, -119, -108, 48, -87, 36, 37, -12, -40, -100, -89, 12, 95, 74, -104, 84, -14, 70, 28, 35, 41, -31, -60, -31, 75, 9, -109, 74, 82, 66, -113, -51, 121, -54, -16, -91, -124, -125, -32, -114, 51, 109, -76, 66, 82, 66, -113, -51, -39, 30, 100, -24, 76, -92, -124, -125, -32, -114, 51, 109, -76, 66, 94, -66, 59, 106, -107, 11, -48, 126, -72, -111, -43, 38, 17, 91, 77, -19, -24, -93, -15, 77, -108, -45, -107, 49, 118, -9, -57, -14, -113, -53, 5, -40, -84, -12, 46, -90, 29, -93, -5, -54, -71, 68, 45, 107, -105, -109, 80, 23, 56, 14, 63, -3, -87, -109, -121, -97, -1, 44, 28, 127, -7, -26, -60, -29, -81, -33, -98, 57, 61, -18, -30, 120, 90, 25, -14, 112, -118, 121, -118, -4, -31, -21, 30, -112, -55, -75, -15, 35, -69, 63, -82, 41, -31, -29, -21, -33, 6, 105, 39, 59, -64, -18, -2, -72, 26, 9, 15, 23, -21, -36, -91, 122, 72, -3, 6, 73, -8, -30, 33, -12, 75, 9, -73, -50, 90, 36, -76, 99, 29, 99, 119, 127, 92, -127, -124, 15, -81, 61, -76, -59, -75, 30, 82, -65, 74, 9, -67, -121, -48, 47, 37, -36, 58, 87, -109, 80, 35, 104, 28, 89, 99, -121, -69, 19, 119, 111, -23, -7, 0, -101, -107, -34, -27, 114, -82, 68, 119, -41, -126, -70, 75, 90, 87, 41, -95, 64, 15, 105, 87, 37, 41, -31, 32, 56, 120, 49, -116, 80, -49, 53, 37, 100, 16, 93, 16, 96, 111, 124, -68, -74, -124, 71, 120, -24, 46, 15, 125, 30, 98, -26, 60, -34, 67, -40, 85, 73, 74, 56, 8, 55, 117, -67, 48, 66, 61, 87, -106, -48, -82, -79, 11, 2, 100, -67, 26, 85, 122, 62, -64, 102, -91, 119, 49, -91, 24, 123, -117, 75, 79, 31, -37, 66, 83, -65, 88, -62, 123, -29, -125, -3, -44, 84, 92, 34, -76, 46, 37, -100, 2, 59, 30, -61, 8, -11, 92, 77, 66, 119, -117, 44, -72, -81, -8, -19, 96, -78, 94, -115, 42, 61, 31, 96, -77, -46, -69, 108, 50, 53, -40, 80, 71, 35, -98, -69, -44, 66, 83, -65, 94, 9, -69, 61, -124, 126, 41, -31, 28, -80, -29, 49, -116, 80, 79, 74, 120, -66, -53, 38, 83, 67, 103, 52, 13, -88, -49, 104, -95, -87, 95, -115, -124, -22, 97, 74, -72, 48, -20, 120, 12, 35, -44, 83, -4, 87, 20, 37, 57, -19, -100, 29, 95, -53, 115, -68, -116, -90, -114, 99, 48, -36, 58, -75, 14, -51, 76, -125, -60, -24, -115, 114, 73, -27, 28, -18, -57, 0, -12, 73, 70, 118, -15, 29, 92, -39, 121, -93, 30, -121, -6, -59, 18, -34, -67, 127, 43, -8, -65, 18, 11, -48, 58, 65, -66, 113, 122, -78, -21, -2, 20, -22, -3, -37, -45, 3, 121, -110, 17, -50, -106, 94, -119, 87, -97, 114, 27, 88, -79, 75, -35, -4, 40, -57, -72, 27, -39, -48, 74, -40, 104, -37, 110, -62, -107, 100, 46, 9, 15, -49, 31, 75, 116, -42, 9, -45, 21, -76, 106, -107, 103, -48, 27, -27, -110, -42, 57, -74, 37, -95, -5, -5, -16, 105, 88, 11, 80, 63, -53, -109, 93, -49, 127, -47, -23, 74, 70, 16, -104, -55, 50, 80, -65, 57, 36, -44, 75, 54, -76, 18, 54, -38, -74, -101, 112, 37, 105, 38, -31, -63, 88, 39, 79, 62, 60, -1, 110, 5, 110, -87, 55, 118, -62, 26, -15, -34, -50, 56, 114, 73, -21, 28, 27, -110, -112, 6, 62, 13, 107, 1, -118, -25, 36, 116, 43, 25, 65, 96, 38, -53, 64, -3, -102, 75, -88, -113, -39, -51, 122, -40, 104, -37, 110, -62, -107, -92, -91, -124, 15, 23, -21, -12, -88, -36, -49, -34, 56, 8, 70, 112, 113, -28, -110, -42, 77, -108, 112, 52, -110, 21, -89, 1, -8, 113, 20, 104, -32, 121, 46, -95, -115, 85, 43, -64, 45, 99, 4, -127, -103, -60, 80, -101, 24, 70, -120, -29, -96, 92, 103, 56, 33, 49, -68, -111, 45, -69, 22, -115, 37, -108, 39, -27, -103, -8, -100, 44, 83, 12, 35, -72, 56, 114, 73, -21, 54, 45, 97, -57, -116, 66, 27, 103, 87, 9, -9, -125, 53, -116, 112, 11, 18, -22, 37, -5, 117, 69, -102, 73, -88, -42, -23, 51, -121, -16, 91, 76, 44, -109, -83, -114, 125, 70, -54, -57, 8, 110, -91, 92, -46, -70, -119, 18, 50, -126, -67, 61, -64, 38, 31, 12, 13, -57, -111, -24, 68, 30, -53, -33, 80, -95, 117, 52, -48, 122, -56, 8, -29, 36, 124, -7, 81, -98, 58, 24, 65, -96, 126, 23, 124, -59, 4, 22, 60, -90, -13, 94, -74, -75, 23, 70, -114, 67, 113, 37, 105, 41, -95, -88, 114, 122, 70, 47, -125, 98, -79, -84, -82, 64, 114, -87, 67, -52, 8, 46, -114, 92, -78, 10, -114, -51, 73, 104, 13, 60, 78, -112, -16, -9, 95, -65, -3, -3, -7, 71, 79, 111, 92, 66, 125, -122, 61, -83, -127, -111, -29, 104, 92, 73, 106, 37, 124, -38, -26, -25, -89, 27, -36, -7, -97, -57, -24, 69, 66, 57, 103, 74, 120, 68, -2, 60, -23, 101, -127, 31, -57, -50, -23, -44, 33, -90, 54, -79, -124, -14, -46, 73, -65, -1, -2, -29, 47, 39, 78, 15, 2, 3, 31, -80, 123, 47, -44, 44, -122, 17, 4, -72, 55, -81, -124, 108, 104, 37, -116, 28, 7, -28, 74, -14, 70, -2, -33, -96, -106, -77, 126, -81, -49, 124, -1, -117, -81, 66, 9, -103, 57, -15, -16, 4, -73, -68, 46, 44, 83, 91, 56, 118, 49, 28, -57, 120, 40, -87, -39, 56, -118, -97, -99, 98, -57, 74, -28, 56, 47, -1, -18, -15, 100, -5, -13, 51, -18, 85, -34, 24, -61, 14, -58, 112, 32, 5, 118, 106, 28, 110, -93, -34, -65, 36, 74, -40, -36, -86, 36, 60, 25, -88, -16, 120, 14, 77, 43, 37, -84, -127, -6, 93, 69, -62, 19, 83, 36, -44, -29, -92, -124, -107, -40, -36, -38, 75, -8, -16, -38, 67, 110, 127, 93, 88, -42, -74, 80, -77, 24, -22, -73, -80, -124, -34, 67, -20, -40, -117, -98, -59, 26, 104, 61, -76, -81, -14, -10, 24, 118, 48, -122, -45, 40, -80, 83, -29, 112, 27, -83, 75, 66, 23, 122, 116, 114, 115, -61, -78, -74, -123, -102, -59, 112, -20, 46, -61, -25, 87, 10, 86, -98, 41, -24, -9, 105, -124, 23, 63, -79, 99, 37, 42, -34, -17, -89, -81, 60, -97, -95, -124, -89, 39, 121, 99, 12, 43, 28, -61, -114, -73, -19, -69, 6, -100, 56, -25, -42, -99, -58, 18, -38, -24, -29, -110, -101, 27, -106, -75, 45, 28, -93, 24, -22, -73, -92, -124, 29, 30, 98, -57, 26, 104, -96, -13, 80, 95, -30, -67, 49, -84, 112, 12, 59, -34, -74, -17, 26, -48, 14, -7, -120, 57, -73, -18, 52, -109, -112, -7, 9, -36, -2, -70, -80, -84, 109, -31, 24, -115, -61, 126, 82, 103, 81, 121, 38, 34, -33, 41, 21, 94, -7, -119, 29, -21, 81, -9, -2, -5, -49, -17, 84, 66, -9, 42, -17, -118, -7, -35, -4, 54, -80, 26, -40, -111, -74, -24, 32, -55, 120, -81, 78, 66, -90, -8, -112, 18, -114, -123, -29, 120, 30, -54, 70, -24, 59, 22, -62, 116, 9, -83, -127, -126, -75, 78, 95, -30, -115, 49, -44, 44, -122, 29, 105, -117, 14, -110, -116, -9, 26, 37, -44, 44, -11, 121, 110, 127, 93, 88, -42, -74, 80, -89, 113, 112, 28, -49, 67, -39, 8, -111, 80, 61, 108, 37, -95, 26, -104, 18, -58, 88, 119, -2, 31, 90, 32, 0, -22, 61, 85, 7, -58, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126
    };

    public static StatusPair<String> validateImage(byte[] image) {
        try {
            String mimeType = tika.detect(image);

            final boolean isImage = mimeType.startsWith("image/");
            if (isImage) {
                String imageFileExtension = mimeType.substring("image/".length());
                return StatusPair.ofTrue(imageFileExtension);
            }

            return StatusPair.ofFalse();
        } catch (IllegalStateException e) {
            Log.error(e.getMessage());
            return StatusPair.ofFalse();
        }
    }
}
