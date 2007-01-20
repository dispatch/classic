#!/usr/bin/ruby

# standard libraries
require 'webrick'
require 'xmlrpc/server'

require 'rubygems' # or otherwise obtain redcloth

WEBrick::Daemon.start # forks process

s = XMLRPC::Server.new(8180)
# listens on localhost only
# If needed extertnally, pass in IP address as second parameter

s.add_handler("redcloth.to_html") do |input|
	require 'redcloth'
  RedCloth.new(input).to_html
end

s.add_handler("bluecloth.to_html") do |input|
	require 'bluecloth'
  BlueCloth.new(input).to_html
end

s.add_handler("rubypants.to_html") do |input|
	require 'rubypants'
  RubyPants.new(input).to_html
end

s.add_handler("bluecloth.rubypants.to_html") do |input|
	require 'bluecloth'
	require 'rubypants'
  RubyPants.new(BlueCloth.new(input).to_html).to_html
end

s.set_default_handler do |name, *args|
  raise XMLRPC::FaultException.new(-99, "Method #{name} missing" +
                                   " or wrong number of parameters")
end

s.serve
