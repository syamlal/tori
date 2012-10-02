package org.vaadin.tori;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

import org.vaadin.tori.indexing.ToriIndexableApplication;

import com.vaadin.server.Constants;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinPortlet;
import com.vaadin.server.VaadinPortletRequest;
import com.vaadin.server.VaadinSessionInitializationListener;
import com.vaadin.server.VaadinSessionInitializeEvent;

public class ToriPortlet extends VaadinPortlet {

    private static final String PORTAL_UTIL_CLASS = "com.liferay.portal.util.PortalUtil";

    @Override
    protected void handleRequest(final PortletRequest request,
            final PortletResponse response) throws PortletException,
            IOException {
        final HttpServletRequest servletRequest = getServletRequest(request);
        if (servletRequest != null && request instanceof RenderRequest
                && ToriIndexableApplication.isIndexerBot(servletRequest)
                && ToriIndexableApplication.isIndexableRequest(servletRequest)) {

            final ToriIndexableApplication app = new ToriIndexableApplication(
                    request);
            final String htmlPage = app.getResultInHtml(servletRequest);

            final RenderResponse renderResponse = (RenderResponse) response;
            renderResponse.setContentType("text/html");
            final OutputStream out = renderResponse.getPortletOutputStream();
            final PrintWriter outWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(out, "UTF-8")));
            outWriter.print(htmlPage);
            outWriter.close();
        } else {
            super.handleRequest(request, response);
        }
    }

    private static HttpServletRequest getServletRequest(
            final PortletRequest request) {
        try {
            final Class<?> portalUtilClass = Class.forName(PORTAL_UTIL_CLASS);

            /* first get the given servletrequest */
            final HttpServletRequest httpServletRequest = (HttpServletRequest) portalUtilClass
                    .getMethod("getHttpServletRequest", PortletRequest.class)
                    .invoke(null, request);

            /*
             * but since that's some kind of a fake request, we need the
             * original one.
             */
            final HttpServletRequest originalHttpServletRequest = (HttpServletRequest) portalUtilClass
                    .getMethod("getOriginalServletRequest",
                            HttpServletRequest.class).invoke(null,
                            httpServletRequest);

            return originalHttpServletRequest;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @SuppressWarnings("serial")
    protected VaadinPortletRequest createVaadinRequest(
            final PortletRequest request) {
        VaadinPortletRequest wrapped = super.createVaadinRequest(request);

        final String portalInfo = request.getPortalContext().getPortalInfo()
                .toLowerCase();
        if (portalInfo.contains("liferay")) {
            wrapped = new VaadinLiferayRequest(request,
                    wrapped.getVaadinService()) {
                @Override
                public String getPortalProperty(final String name) {
                    if (Constants.PORTAL_PARAMETER_VAADIN_RESOURCE_PATH
                            .equals(name)) {
                        return request.getContextPath();
                    }
                    return super.getPortalProperty(name);
                }
            };
        }
        return wrapped;
    }

    @Override
    protected void portletInitialized() {
        getVaadinService().addVaadinSessionInitializationListener(
                new VaadinSessionInitializationListener() {
                    @Override
                    public void vaadinSessionInitialized(
                            final VaadinSessionInitializeEvent event)
                            throws ServiceException {
                        String theme = getInitParameter("theme");
                        theme = (theme != null) ? theme : "tori-liferay";
                        event.getVaadinSession().addUIProvider(
                                new ToriUiProvider(theme));
                    }
                });
    }
}
