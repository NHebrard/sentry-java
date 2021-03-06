package io.sentry.appengine;

import com.google.appengine.api.utils.SystemProperty;
import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.appengine.connection.AppEngineAsyncConnection;
import io.sentry.appengine.event.helper.AppEngineEventBuilderHelper;
import io.sentry.config.Lookup;
import io.sentry.connection.Connection;
import io.sentry.dsn.Dsn;

/**
 * SentryClientFactory dedicated to create async connections within Google App Engine.
 */
public class AppEngineSentryClientFactory extends DefaultSentryClientFactory {
    /**
     * Option for the queue name used in Google App Engine of threads assigned for the connection.
     */
    public static final String QUEUE_NAME = "sentry.async.gae.queuename";
    /**
     * Option to define the identifier of the async connection across every instance of the application.
     * <p>
     * It is important to set a different connection identifier for each opened connection to keep the uniqueness
     * of connection ID.
     * <p>
     * If the connection identifier is not specified, the system will define a connection identifier itself, but its
     * uniqueness within an instance isn't guaranteed.
     *
     * @see AppEngineAsyncConnection
     */
    public static final String CONNECTION_IDENTIFIER = "sentry.async.gae.connectionid";

    /**
     * This is provided for backwards compatibility but doesn't support custom lookup injection.
     * <p>
     * This uses a default lookup instance, use {@link #AppEngineSentryClientFactory(Lookup)} if you need to
     * pass a specially configured lookup.
     */
     public AppEngineSentryClientFactory() {
        this(Lookup.getDefault());
    }

    /**
     * Creates a new instance configured using the provided lookup instance.
     *
     * @param lookup the lookup instance to load configuration from
     */
    public AppEngineSentryClientFactory(Lookup lookup) {
        super(lookup);
    }

    @Override
    public SentryClient createSentryClient(Dsn dsn) {
        SentryClient sentryClientInstance = super.createSentryClient(dsn);
        sentryClientInstance.addBuilderHelper(new AppEngineEventBuilderHelper());
        return sentryClientInstance;
    }

    /**
     * Encapsulates an already existing connection in an {@link AppEngineAsyncConnection} and get the async options
     * from the Sentry DSN.
     *
     * @param dsn        Data Source Name of the Sentry server.
     * @param connection Connection to encapsulate in an {@link AppEngineAsyncConnection}.
     * @return the asynchronous connection.
     */
    @Override
    protected Connection createAsyncConnection(Dsn dsn, Connection connection) {
        String connectionIdentifier = lookup.get(CONNECTION_IDENTIFIER, dsn);
        if (connectionIdentifier == null) {
            connectionIdentifier = AppEngineSentryClientFactory.class.getCanonicalName()
                + dsn + SystemProperty.version.get();
        }

        AppEngineAsyncConnection asyncConnection = new AppEngineAsyncConnection(connectionIdentifier, connection);

        String queueName = lookup.get(QUEUE_NAME, dsn);
        if (queueName != null) {
            asyncConnection.setQueue(queueName);
        }

        return asyncConnection;
    }
}
