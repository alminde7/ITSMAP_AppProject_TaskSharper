package tasksharper.tasksharperclient.Models;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.IOException;

public class AuthErrorEvent{
    public UserRecoverableAuthIOException exception;
    public AuthErrorEvent(UserRecoverableAuthIOException ex){
        exception = ex;
    }
}
