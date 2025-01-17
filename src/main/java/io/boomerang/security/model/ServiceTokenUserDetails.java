package io.boomerang.security.model;

public class ServiceTokenUserDetails extends UserToken {

  private final String accessToken;

  public ServiceTokenUserDetails(String email, String firstName, String lastName,
      String accessToken) {
    super(email, firstName, lastName);

    this.accessToken = accessToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

}
