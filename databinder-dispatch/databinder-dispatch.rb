#!/usr/bin/ruby

require 'rubygems'
require 'xmlrpc/server'
require 'redcloth'

s = XMLRPC::Server.new(8180, "127.0.0.1") 

s.add_handler("redcloth.to_html") do |input|
  r = RedCloth.new input
  r.to_html
end

s.set_default_handler do |name, *args|
  raise XMLRPC::FaultException.new(-99, "Method #{name} missing" +
                                   " or wrong number of parameters!")
end

s.serve
