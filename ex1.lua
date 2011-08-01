
local http = require("socket.http")
--require "socket.http.get"

 -- defines a factorial function
    function fact (n)
      if n == 0 then
        return 1
      else
        return n * fact(n-1)
      end
    end

    print("enter a number:")
    a = io.read("*number")        -- read a number
    print(fact(a))
	b, h, c, e = http.request("http://www.tecgraf.puc-rio.br/luasocket/http.html")
	print (b,h,c,e)