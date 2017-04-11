require 'net/https'
require 'uri'


module HttpHelper

  def response
    @response ||= fetch
  end

  def request(uri)
    if uri.query then
      p="#{uri.path}?#{uri.query}"
    else
      p=uri.path
    end

    case @method
      when 'HEAD' then Net::HTTP::Head.new(p, @headers)
      when 'GET' then Net::HTTP::Get.new(p, @headers)
      else raise "Unexpected @method: #{@method}"
    end
  end

  def fetch
    is_redirecting = true
    redirect_count = 0
    while is_redirecting and redirect_count < 10
      uri = URI.parse(@url)
      connection_uri = uri

      unless !@prefire_response.nil? or @prefire_url.nil?
        prefire_uri = URI.parse(@prefire_url)
        prefire_uri.host.should == uri.host
        prefire_uri.port.should == uri.port
      end

      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = true if uri.scheme == 'https'
      http.start {
        unless !@prefire_response.nil? or prefire_uri.nil?
          http.request(request(prefire_uri)) {|res|
            @prefire_response = res
            res
          }
        end

        # Do same-host redirects inside keepalive
        while is_redirecting and uri.host == connection_uri.host and uri.port == connection_uri.port and redirect_count < 10
          http.request(request(uri)) {|res|
            @response  = res
            if @expected_responses[@url]
              res.code.should == @expected_responses[@url].to_s
            end

            if @follow_redirects && res.key?("location") then
              @url = res["location"]
              uri = URI.parse(@url)
              redirect_count += 1
            else
              is_redirecting = false
            end

            res
          }
        end
      }
    end

    redirect_count.should < 10

    @response
  end
end

World(HttpHelper)

Before do
  @expected_responses = {}
  @follow_redirects = false
end