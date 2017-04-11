Feature: Redirect users to HTTPS
  As a paranoid user
  I want the illusion of security by Tabula being over https

  Scenario: User accesses Tabula through HTTP
    When I request http://@HOST@/
    Then the location response header should be "https://@HOST@/"
    And the status code should be 301

  Scenario: User accesses Tabula through HTTPS
    When I request https://@HOST@/
    Then the status code should be 200