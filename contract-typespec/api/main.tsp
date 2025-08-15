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
import "./graphs.tsp";
import "./prometheus.tsp";

import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi";

using TypeSpec.OpenAPI;
using Http;
using Rest;

@service(#{ title: "Kafbat UI Api Service" })
@info(#{
  contact: #{ name: "API Support", email: "support@kafbat.io" },
  license: #{ name: "Apache 2.0", url: "https://www.apache.org/licenses/LICENSE-2.0.html" },
  version: "0.2.0"
})
@server("http://localhost:8080", "Default endpoint for Kafbat UI API")
namespace Api;

