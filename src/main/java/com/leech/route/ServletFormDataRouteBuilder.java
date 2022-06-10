package com.leech.route;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpMessage;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ServletFormDataRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("servlet:formData?matchOnUriPrefix=true&servletName=CamelServlet").process(new Processor() {
            public void process(Exchange exchange) throws Exception {
                Message in = exchange.getIn();
                Message out = exchange.getMessage();
                HttpMessage httpMessage = (HttpMessage)in;
                Map<String, Object> headers = httpMessage.getHeaders();
                Object camelHttpMethod = headers.get("CamelHttpMethod");

                HttpServletRequest request = httpMessage.getRequest();
                String currentRequestMethod = request.getMethod();
                log.info("requestMethod-->{}", currentRequestMethod);

                InputStream body = (InputStream)in.getBody();
                String result = IOUtils.toString(body, StandardCharsets.UTF_8);
                System.out.println(result);
                try {
                    FileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload sf = new ServletFileUpload(factory);
                    HttpServletRequest req = (HttpServletRequest) headers.get(Exchange.HTTP_SERVLET_REQUEST);
                    if (!ServletFileUpload.isMultipartContent(req)) {
                        throw new Exception("no multipartcontent");
                    }
                    List<FileItem> formData = sf.parseRequest(req);
                    for (FileItem fi : formData) {
                        if (fi.isFormField()) {
                            System.out.println("field_name:" + fi.getFieldName() + ":" + fi.getString("UTF-8"));
                            switch (fi.getFieldName()) {
                                case "name":
                                    System.out.println("receive name");
                                    break;
                                default:
                                    System.out.println("unknow data");
                            }
                        } else {
                            String image_name = fi.getName();
                            System.out.println("image_name:" + image_name);
                            if (image_name != "") {
                                String image_dir_path = req.getServletContext().getRealPath("/images/");
                                File image_dir = new File(image_dir_path);
                                if (!image_dir.exists()) {
                                    image_dir.mkdir();
                                }
                                String file_name = UUID.randomUUID().toString();
                                String suffix = image_name.substring(fi.getName().lastIndexOf("."));
                                System.out.println("image_dir_path:" + image_dir_path);
                                System.out.println("file_name:" + file_name);
                                System.out.println("suffix:" + suffix);
                                fi.write(new File(image_dir_path, file_name + suffix));
                            } else {
                                throw new Exception("no file receive");
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
                String path = exchange.getIn().getHeader(Exchange.HTTP_URI, String.class);
                path = path.substring(path.lastIndexOf("/"));

                //assertEquals(CONTENT_TYPE, contentType, "Get a wrong content type");
                // assert camel http header
                String charsetEncoding = exchange.getIn().getHeader(Exchange.HTTP_CHARACTER_ENCODING, String.class);
                //assertEquals(charsetEncoding, "Get a wrong charset name from the message heaer", "UTF-8");
                // assert exchange charset
                //assertEquals(exchange.getProperty(Exchange.CHARSET_NAME), "Get a wrong charset naem from the exchange property", "UTF-8");
                exchange.getOut().setHeader(Exchange.CONTENT_TYPE, contentType + "; charset=UTF-8");
                exchange.getOut().setHeader("PATH", path);
                exchange.getOut().setBody("<b>Hello World</b>");
            }
        }).convertBodyTo(String.class);
    }
}