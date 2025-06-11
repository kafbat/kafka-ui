// APIs
import "./clusters.tsp";
import "./brokers.tsp";
import "./topics.tsp";
import "./messages.tsp";
import "./consumer-groups.tsp";
import "./schemas.tsp";
import "./kafka-connect.tsp";
import "./ksql.tsp";
import "./acls.tsp";
import "./quotas.tsp";
import "./auth.tsp";
import "./config.tsp";
import "@typespec/openapi";

using TypeSpec.OpenAPI;

@service(#{ title: "Kafbat UI Api Service" })
@info(#{
  contact: #{ name: "API Support", email: "support@kafbat.io" },
  license: #{ name: "Apache 2.0", url: "https://www.apache.org/licenses/LICENSE-2.0.html" },
  version: "0.2.0"
})
namespace Api;

