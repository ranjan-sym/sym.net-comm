package net.symplifier.comm.http;

import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.Port;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Created by ranjan on 6/10/15.
 */
public class StaticFileRequestHandler implements HTTPRequest.Handler {

  private final File rootFolder;
  private final String defaultFile;

  public StaticFileRequestHandler(File wwwroot) {
    this(wwwroot, "index.html");
  }

  public StaticFileRequestHandler(File wwwroot, String defaultFile) {
    this.rootFolder = wwwroot;
    this.defaultFile = defaultFile;
  }

  @Override
  public Port.Responder onRequest(HTTPRequest request, HTTPResponse response) {
    File f = new File(rootFolder, request.relativePath);
    if (f.isDirectory()) {
      f = new File(f, defaultFile);
    }
    if (f.isFile()) {
      // Detect the content type and respond accordingly
      String filename = f.getName();
      String contentType = "text/html";
      if (filename.endsWith(".html") || filename.endsWith(".htm")) {
        contentType = "text/html";
      } else if(filename.endsWith(".jpg") || filename.endsWith(".png") || filename.endsWith(".gif")) {
        contentType = "text/image";
      }
      response.setHeader("Content-Type", contentType);
      response.setContentLength(f.length());
      return new FileResponder(f);
    } else {
      response.setResponse(400, "Not Found");
      response.setResponseText("The resource " + request.basePath + request.relativePath + " you were looking for was not available on this server");
      return null;
    }
  }

  private static class FileResponder implements Port.Responder {

    FileChannel fileChannel;
    public FileResponder(File file) {
      RandomAccessFile f = null;
      try {
        f = new RandomAccessFile(file, "r");
        fileChannel = f.getChannel();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

    }

    @Override
    public boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter) {
      int n = 0;
      try {
        n = fileChannel.read(transmitter.getBuffer());
      } catch (IOException e) {
        e.printStackTrace();
      }
      return (n == -1);
    }

    @Override
    public void onTransmissionComplete(DigitalPort port) {
      try {
        fileChannel.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
