Given /^there is a file|page at (.+?)$/ do |url|
  (@expected_responses||={})[url] = 200
end

When /^I request (\S+?) then (\S+?)$/ do |prefire_url, url|
  @method = 'GET'
  @prefire_url = prefire_url
  @url = url
  @request = nil
end

When /^I request (\S+?) twice$/ do |url|
  @method = 'GET'
  @prefire_url = url
  @url = url
  @request = nil
end

When /^I request (\S+?)$/ do |url|
  @method = 'GET'
  @url = url
  @request = nil
end

When /^I make a HEAD request to (.+?)$/ do |url|
  @method = 'HEAD'
  @url = url
  @request = nil
end

When /^I set the header (.+?) to "([^\"]*)"$/ do |header, value|
  (@headers||={})[header] = value
end

When /^I set the header (.+?) as (.+?)$/ do |header, stored|
  (@headers||={})[header] = @stored_headers[stored.downcase]
end

When /^I follow redirects$/ do
  @follow_redirects = true
end

Then /^the (.+?) response header should be "(.*)"$/ do |header, value|
  response[header.downcase].should == value
end

Then /^the (.+?) response header should start with "([^\"]*)"$/ do |header, value|
  response[header.downcase].should =~ Regexp::new("^" + value, "i")
end

Then /^the (.+?) response header should not exist$/ do |header|
  response.key?(header.downcase).should == false
end

Then /^the (.+?) response header should exist$/ do |header|
  response.key?(header.downcase).should == true
end

Then /^the (.+?) response header should not equal$/ do |header, text|
  response[header.downcase].eql?(text).should == false
end

Then /^the (.+?) response header should be the same as the stored (.+?)$/ do |header, stored|
  response[header.downcase].should == @stored_headers[stored.downcase]
end

Then /^the (.+?) response header should be stored$/ do |header|
  (@stored_headers||={})[header.downcase] = response[header.downcase]
end

Then /^the status code should be ([0-9]*)$/ do |status|
  response.code.should == status
end

Then /^the response body should include the text "([^\"]*)"$/ do |text|
  response.body.include?(text).should == true
end

Then /^the response body should include the text '([^\']*)'$/ do |text|
  response.body.include?(text).should == true
end

Then /^the response body should not include the text "([^\"]*)"$/ do |text|
  response.body.include?(text).should == false
end

Then /^the response body should not include the text '([^\']*)'$/ do |text|
  response.body.include?(text).should == false
end

Then /^the final url should be "([^\"]*)"$/ do |value|
  @url.should == value
end




Then /^I should tidy up$/ do
  @response = nil
  @expected_responses = {}
  @headers = {}
end
