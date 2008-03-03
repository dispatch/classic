#!/usr/bin/ruby

# standard libraries
require 'webrick'
include WEBrick

require 'rubygems' # or otherwise obtain redcloth, etc.

PID = '/var/run/databinder-dispatch.pid'

def pid(new_id = false)
  if new_id
    File.open(PID, 'w') do |f| f << new_id end
  else 
    File.exist?(PID) && IO.read(PID).to_i
  end
end

def start()
  puts "Warning, existing pid record: " + pid.to_s if pid
  pid(-1)         # test that we can write before forking
  WEBrick::Daemon.start # forks process
  pid(Process.pid)
  run
end

def stop()
  if pid
    begin
      Process.kill("INT", pid)
    rescue Errno::ESRCH
      puts "server not running at pid: #{pid}; removing record"
    end
    File.unlink PID
  else
    puts "no server id on record"
  end
end

def run()
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
  
  trap("INT") { s.shutdown }
  s.start
end

begin
  case !ARGV.empty? && ARGV[0]
  when 'run'
    run
  when 'start'
    start
  when 'stop'
    stop
  when 'restart'
    stop
    start
  else
    puts "Invalid command. Please specify run, start, stop or restart."
    exit
  end
rescue Errno::EACCES
  puts "Error, unable to write: " + PID
  exit(1)
rescue Errno::EPERM
  puts "Error, not permitted to kill pid: " + pid.to_s
  exit(1)
end
