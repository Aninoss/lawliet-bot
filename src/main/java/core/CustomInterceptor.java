package core;

import constants.RegexPatterns;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CustomInterceptor implements Interceptor {

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (RegexPatterns.INTERACTION.matcher(request.url().encodedPath()).matches()) {
            request = request.newBuilder()
                    .removeHeader("Authorization")
                    .build();
        }

        return chain.proceed(request);
    }

}
