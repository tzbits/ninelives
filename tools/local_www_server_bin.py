"""A wrapper script to run the Python http.server module via Bazel."""
import http.server
import socketserver
import os
import sys

# The directory to serve is the first argument after '--' in the bazel run command.
# The port is the second.
# Example: bazel run //tools:server_bin -- path/to/public 8080
if len(sys.argv) > 1:
    server_directory = os.path.dirname(sys.argv[1])
    if not os.path.isdir(server_directory):
        print(f"Error: Directory '{server_directory}' not found.", file=sys.stderr)
        sys.exit(1)
    os.chdir(server_directory)

# The port is the second argument. Default to 8080 if not provided.
port = 8080
if len(sys.argv) > 2:
    try:
        port = int(sys.argv[2])
    except ValueError:
        print("Error: Port must be a number.", file=sys.stderr)
        sys.exit(1)

handler = http.server.SimpleHTTPRequestHandler
httpd = None # Initialize httpd outside the try block

try:
    httpd = socketserver.TCPServer(("", port), handler)
    print(f"Serving HTTP on port {port} from directory '{os.getcwd()}'...")
    httpd.serve_forever()
except KeyboardInterrupt:
    print("\nKeyboard interrupt received, shutting down gracefully...")
except Exception as e:
    print(f"\nAn error occurred: {e}")
finally:
    if httpd:
        print("Closing the server.")
        httpd.server_close() # This closes the socket and unbinds the port
