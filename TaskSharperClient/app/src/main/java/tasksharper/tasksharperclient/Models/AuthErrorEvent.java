package tasksharper.tasksharperclient.Models;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.IOException;

public class AuthErrorEvent{
    public GoogleAuthIOException exception;
    public AuthErrorEvent(GoogleAuthIOException ex){
        exception = ex;
    }
}
