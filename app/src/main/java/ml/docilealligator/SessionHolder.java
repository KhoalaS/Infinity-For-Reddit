package ml.docilealligator;

import org.matrix.android.sdk.api.session.Session;

public class SessionHolder {
    public static Session currentSession = null;
    public static SessionHolder sessionHolder = new SessionHolder();

    public static SessionHolder getInstance(){
        return sessionHolder;
    }
    public void setCurrentSession(Session session){
        currentSession = session;
    }

    public Session getCurrentSession(){
        return currentSession;
    }
}
