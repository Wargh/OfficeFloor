package net.officefloor.tutorial.scalahttpserver

import org.scalatest.FlatSpec
import net.officefloor.polyglot.scalatest.WoofRules

class ScalaHttpServerTest extends FlatSpec with WoofRules {

  "Call Server" should "get result" in {
    withMockWoofServer { server =>
      val request = mockRequest("/scala")
          .method(httpMethod("POST"))
          .header("Content-Type", "application/json")
          .entity(jsonEntity(new ScalaRequest("Daniel")))
      val response = server.send(request)
      response.assertResponse(200, jsonEntity(new ScalaResponse("Hello Daniel")))
    }
  }

}