#!/usr/bin/ruby

# standard libraries
require 'webrick'
require 'xmlrpc/server'

require 'rubygems' # or otherwise obtain redcloth
require 'redcloth'

WEBrick::Daemon.start # forks process

s = XMLRPC::Server.new(8180)
# listens on localhost only
# If needed extertnally, pass in IP address as second parameter

s.add_handler("redcloth.to_html") do |input|
  r = RedCloth.new input
  r.to_html
end

s.set_default_handler do |name, *args|
  raise XMLRPC::FaultException.new(-99, "Method #{name} missing" +
                                   " or wrong number of parameters!")
end

s.serve
