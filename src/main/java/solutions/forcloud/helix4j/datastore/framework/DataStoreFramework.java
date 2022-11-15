/*
 * The MIT License
 *
 * Copyright (C) 2022 4cloud Solutions.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package solutions.forcloud.helix4j.datastore.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solutions.forcloud.helix4j.HelixConfig;
import solutions.forcloud.helix4j.datastore.DataStoreConfig;
import solutions.forcloud.helix4j.datastore.handlers.DataStoreAccessControlPoint;
import solutions.forcloud.helix4j.datastore.handlers.DataStoreDataChangeProcessor;
import solutions.forcloud.helix4j.datastore.handlers.DataStoreSessionExpiryProcessor;
import solutions.forcloud.helix4j.framework.HelixFramework;
import solutions.forcloud.helix4j.modules.datamanagement.DataManagerIF;
import solutions.forcloud.helix4j.modules.datamanagement.DataStoreManager;

/**
 *
 * @author mpujic
 */
public class DataStoreFramework {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStoreFramework.class);
    
    private static DataStoreConfig dataStoreConfig = null;
    
    public static void powerUp(DataStoreConfig configuration) {
        LOGGER.info("powerUp() called!");
        
        dataStoreConfig = configuration;
        HelixConfig helixConfig = configuration.getHelixConfig();
        DataManagerIF dataManager = new DataStoreManager("USER_%s_PRODUCER_%s_DATASTORE");
        DataStoreSessionExpiryProcessor sessionExpiryProcessor = 
                new DataStoreSessionExpiryProcessor("DATA_STORE_" + helixConfig.getNodeId() + "_SESSION_EXPIRY_PROCESSOR");
        DataStoreAccessControlPoint accessControPoint = 
                new DataStoreAccessControlPoint();
        DataStoreDataChangeProcessor dataChangeProcessor = 
                new DataStoreDataChangeProcessor();
        
        HelixFramework.powerUp(helixConfig,
                               dataManager,
                               accessControPoint, 
                               sessionExpiryProcessor, 
                               dataChangeProcessor);
        
        LOGGER.info("powerUp() exited!");
    }
    
    public static DataStoreConfig getDataStoreConfig() {
        return dataStoreConfig;
    }
    
    public static void powerDown() {
        LOGGER.info("powerDown() called!");

        HelixFramework.powerDown();
        
        LOGGER.info("powerDown() exited!");
    }

}
