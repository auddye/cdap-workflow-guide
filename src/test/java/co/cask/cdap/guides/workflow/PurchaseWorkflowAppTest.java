/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.guides.workflow;

import co.cask.cdap.test.ApplicationManager;
import co.cask.cdap.test.ServiceManager;
import co.cask.cdap.test.StreamManager;
import co.cask.cdap.test.TestBase;
import co.cask.cdap.test.WorkflowManager;
import co.cask.common.http.HttpRequest;
import co.cask.common.http.HttpRequests;
import co.cask.common.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Test case for the PurchaseWorkflowApp.
 */
public class PurchaseWorkflowAppTest extends TestBase {

  @Test
  public void test() throws Exception {
    // Deploy application
    ApplicationManager applicationManager = deployApplication(PurchaseWorkflowApp.class);

    // Send some invalid events through the stream
    StreamManager streamManager = getStreamManager("purchaseEvents");

    streamManager.send("bob bought 3 apples for $30");
    streamManager.send("joe bought 1 apple for $100");
    streamManager.send("joe bought 10 pineapples for $20");
    streamManager.send("cat bought 3 bottles for $12");
    streamManager.send("cat bought 2 pops for $14");

    // Start the Workflow
    WorkflowManager workflowManager = applicationManager.getWorkflowManager("PurchaseWorkflow").start();
    workflowManager.waitForFinish(3, TimeUnit.MINUTES);

    // Start the service
    ServiceManager serviceManager = applicationManager.getServiceManager("PurchaseResultService").start();
    serviceManager.waitForStatus(true);

    // Get the total sale made for the product 'apple'
    URL url = new URL(serviceManager.getServiceURL(), "purchases/products/apple");
    HttpRequest request = HttpRequest.get(url).build();
    HttpResponse response = HttpRequests.execute(request);
    Assert.assertEquals(200, response.getResponseCode());
    Assert.assertEquals("130", response.getResponseBodyAsString());

    // Get the total purchases made by customer 'cat'
    url = new URL(serviceManager.getServiceURL(), "purchases/customers/cat");
    request = HttpRequest.get(url).build();
    response = HttpRequests.execute(request);
    Assert.assertEquals(200, response.getResponseCode());
    Assert.assertEquals("26", response.getResponseBodyAsString());
  }
}
