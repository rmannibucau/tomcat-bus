package com.github.rmannibucau.tomcat.bus.test.cdi.app;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.LinkedList;

@WebServlet(urlPatterns = "/report")
public class ReportServlet extends HttpServlet {
    private static Collection<Message> messages = new LinkedList<Message>();

    protected synchronized void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpURLConnection.HTTP_OK);
        resp.getWriter().write(messages.toString());
    }

    public static synchronized void newMessage(final Message message) {
        messages.add(message);
    }
}
