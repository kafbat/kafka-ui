import "@typespec/openapi";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@tag("Authorization")
interface AuthorizationApi {
  @route("/api/authorization")
  @summary("Get user authorization related info")
  @operationId("getUserAuthInfo")
  @get
  getUserAuthInfo(): AuthenticationInfo;
}

@route("/login")
@summary("Authenticate")
@operationId("authenticate")
@post
@tag("Unmapped")
op authenticate(@header contentType: "application/x-www-form-urlencoded", @body form: LoginForm): void | Http.Response<401>;


model LoginForm {
  username?: string;
  password?: string;
}

model AuthenticationInfo {
  @doc("true if role based access control is enabled and granular permission access is required")
  rbacEnabled: boolean;
  userInfo?: UserInfo;
}

model UserInfo {
  username: string;
  permissions: UserPermission[];
}


model UserPermission {
  clusters: string[];
  resource: ResourceType;
  value?: string;
  actions: Action[];
}
