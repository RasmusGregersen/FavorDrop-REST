import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.internal.NonNull;
import com.google.firebase.tasks.OnCompleteListener;
import com.google.firebase.tasks.OnFailureListener;
import com.google.firebase.tasks.OnSuccessListener;
import com.google.firebase.tasks.Task;
import com.sun.org.apache.regexp.internal.RE;
import com.sun.xml.internal.ws.client.RequestContext;
import com.sun.xml.internal.ws.client.sei.ResponseBuilder;

import javax.annotation.Priority;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by LarsMyrup on 26/04/2017.
 */

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {



    FirebaseAuth auth;

    public AuthenticationFilter() {

        try {
//            File file = new File("/home/ubuntu/serviceAccountKey.json");
            File file = new File("/Users/LarsMyrup/Documents/Workspace/FavorDrop/FavorDrop-REST/serviceAccountKey.json");
            FileInputStream serviceAccount = new FileInputStream(file);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl("https://favordrop.firebaseio.com/")
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            auth = FirebaseAuth.getInstance(app);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Get the HTTP Authorization header from the request
        String authorizationHeader =
                requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Check if the HTTP Authorization header is present and formatted correctly
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new NotAuthorizedException("Authorization header must be provided");
        }

        // Extract the token from the HTTP Authorization header
        String token = authorizationHeader.substring("Bearer".length()).trim();

        try {
            System.out.println("valider token kald");
            validateToken(token, requestContext);

        } catch (Exception e) {
            System.out.println(e);
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }


    }

    private void validateToken(String token, final ContainerRequestContext requestContext) throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        FirebaseAuth.getInstance().verifyIdToken(token)
                .addOnSuccessListener(new OnSuccessListener<FirebaseToken>() {
                    @Override
                    public void onSuccess(FirebaseToken decodedToken) {
                        System.out.println("on success");
                        latch.countDown();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("on fail " + e);
                        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                        latch.countDown();
                }
            });
        latch.await();
    }
}
