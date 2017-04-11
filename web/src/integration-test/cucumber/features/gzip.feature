Feature: GZipped content
  In order to minimise download times
  Suitable mime-types should be GZipped before serving

  Scenario Outline: Request GZipped script in static from various browsers
    Given there is a file at https://@HOST@/static/js/home.js.484115321229
    When I request https://@HOST@/static/js/home.js.484115321229
    And I set the header accept-encoding to "gzip"
    And I set the header user-agent to "<user_agent>"
    Then the content-encoding response header <expected_encoding>

  Examples:
    | user_agent                                                                                               | expected_encoding |
    | Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36 | should be "gzip"  |