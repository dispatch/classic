#!/usr/bin/ruby

# standard libraries
require 'webrick'
include WEBrick

require 'rubygems' # or otherwise obtain redcloth, etc.

WEBrick::Daemon.start # forks process

s = HTTPServer.new( :Port => 8180 )
# listens on localhost only
# If needed extertnally, pass in IP address as second parameter

def input(req) req.query['input'] end

s.mount_proc('/redcloth') do |req, resp|
	require 'redcloth'
	resp.body = RedCloth.new(input(req)).to_html
end

s.mount_proc('/maruku') do |req, resp|
	require 'maruku'
	resp.body = Maruku.new(input(req)).to_html
end

s.mount_proc('/rubypants') do |req, resp|
	require 'rubypants'
	resp.body = RubyPants.new(input(req)).to_html
end

s.start
