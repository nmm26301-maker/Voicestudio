package com.voice.studio;
import java.io.*;
import java.net.*;
import java.util.*;
/** Mock health endpoint, checks read-only method, auth and bounded parsing. */
public class CloudHealthTest {
 static int checks;
 static void check(boolean v){checks++;if(!v)throw new AssertionError();}
 static class Connection extends HttpURLConnection {
  byte[] body;int status=200;boolean closed;
  Connection(String data)throws Exception{super(new URL("https://gateway.invalid/v1/health"));body=data.getBytes("UTF-8");}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){closed=true;}
  public int getResponseCode(){return status;}
  public InputStream getInputStream(){return new ByteArrayInputStream(body);}
  public OutputStream getOutputStream(){throw new AssertionError("Health must never submit a paid POST");}
 }
 public static void main(String[] args)throws Exception{
  Connection c=new Connection("{\"status\":\"ok\",\"configured\":[\"qwen3tts\",\"fish\",\"invented\",\"auto\"]}");
  Set<String> found=new CloudHealthClient(url->{check(url.toString().equals("https://gateway.invalid/v1/health"));return c;}).check("https://gateway.invalid/v1","fixture");
  check(found.equals(new HashSet<>(Arrays.asList("qwen3tts","fish"))));check("GET".equals(c.getRequestMethod()));check("Bearer fixture".equals(c.getRequestProperty("Authorization")));check(c.closed&&!c.getInstanceFollowRedirects());
  for(int code:new int[]{301,401,403,503}){Connection bad=new Connection("{}");bad.status=code;boolean failed=false;try{new CloudHealthClient(url->bad).check("https://gateway.invalid","fixture");}catch(IOException e){failed=true;}check(failed&&bad.closed);}
  for(String data:new String[]{"{}","{\"status\":\"failed\"}","{\"status\":\"ok\",\"configured\":null}",String.join("",Collections.nCopies(33000,"x"))}){Connection bad=new Connection(data);boolean failed=false;try{new CloudHealthClient(url->bad).check("https://gateway.invalid","fixture");}catch(Exception e){failed=true;}check(failed&&bad.closed);}
  for(String address:new String[]{"http://gateway.invalid","https://user:secret@gateway.invalid","https://gateway.invalid?key=x"}){boolean failed=false;try{new CloudHealthClient(url->{throw new AssertionError("Invalid origin opened");}).check(address,"fixture");}catch(IOException e){failed=true;}check(failed);}
  System.out.println("Cloud health: "+checks+" mock read-only/auth/redirect/body-limit checks passed.");
 }
}
