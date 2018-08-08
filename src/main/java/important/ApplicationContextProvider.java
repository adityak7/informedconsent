package important;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * @author guoxiwang
 * @reference http://www.java2novice.com/spring/application-context-object/
 */
public class ApplicationContextProvider implements ApplicationContextAware {


    private static ApplicationContextProvider instance;
    private static ApplicationContext context;

    public static ApplicationContextProvider getInstance() {
        if (instance == null) {
            instance = new ApplicationContextProvider();
        }
        return instance;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac)
            throws BeansException {
        context = ac;
    }


}
