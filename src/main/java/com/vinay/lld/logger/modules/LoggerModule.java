package com.vinay.lld.logger.modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.vinay.lld.logger.service.LoggingService;
import com.vinay.lld.logger.service.impl.LoggingServiceImpl;
import com.vinay.lld.logger.storage.Storage;
import com.vinay.lld.logger.storage.impl.FileStorage;

public class LoggerModule extends AbstractModule {

    @Override
    public void configure() {
        bind(String.class).annotatedWith(Names.named("logFileName")).toInstance("Application.log");
        bind(Integer.class).annotatedWith(Names.named("logFlushInterval")).toInstance(2);
        bind(LoggingService.class).to(LoggingServiceImpl.class).asEagerSingleton();
        bind(Storage.class).to(FileStorage.class).asEagerSingleton();
    }
}
