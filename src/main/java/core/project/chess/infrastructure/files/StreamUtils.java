package core.project.chess.infrastructure.files;

import core.project.chess.domain.commons.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class StreamUtils {

    public Result<byte[], Throwable> toByteArray(InputStream input) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = input.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            input.close();
            return Result.success(buffer.toByteArray());
        } catch (IOException e) {
            return Result.failure(e);
        }
    }
}
