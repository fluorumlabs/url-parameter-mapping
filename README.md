[![Published on Vaadin  Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/url-parameter-mapping)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/url-parameter-mapping.svg)](https://vaadin.com/directory/component/url-parameter-mapping)

# URL Parameter Mapping for Vaadin Flow

While https://github.com/vaadin/flow/issues/2740 and 
https://github.com/vaadin/flow/issues/4213 are still in the works, 
the need for flexible parametrized routes still exist. This
helper implementation lives on top of built in HasUrlParameter
and provides support for named parameters.

Installing with Maven:
```xml
<repository>
   <id>vaadin-addons</id>
   <url>http://maven.vaadin.com/vaadin-addons</url>
</repository>
```

```xml
<dependency>
   <groupId>org.vaadin.helper</groupId>
   <artifactId>url-parameter-mapping</artifactId>
   <version>1.0.0-alpha5</version>
</dependency>
```

Usage example:
```java
import org.vaadin.flow.helper.*;

...

@Route("example")
@UrlParameterMapping(":exampleId/:orderId")
// Will match /example/12345/ORD223434, set exampleId = 12345 and
// call setOrder("ORD223434")
// Otherwise user will be rerouted to default NotFoundException view
class MyView extends Div implements HasUrlParameterMapping {
    // Note: parameter fields/setters should be public    
    @UrlParameter
    public Integer exampleId;
    
    @UrlParameter(name = "orderId", regEx = "ORD[0-9]{6}") 
    public setOrder(String order) { ... }
    ...
}
```  

Optional parameters are supported:
```java
@Route("example")
@UrlParameterMapping(":exampleId[/:version]")
// Will match /example/12345 and /example/12345/678
// version property will receive null when missing 
```

Static segments are supported:
```java
@Route("example")
@UrlParameterMapping("edit/:exampleId")
// Will match /example/edit/12345
```

Parameters can be anywhere:
```java
@Route("order")
@UrlParameterMapping("detail/:orderId/edit")
// Will match /order/detail/12345/edit
```

Those could be also optional:
```java
@Route("order")
@UrlParameterMapping("detail[/:rowId]/edit")
// Will match /order/detail/12345/edit and /order/detail/edit
```

Full route paths can also be used for matching:
```java
import org.vaadin.flow.helper.*;

...

@Route("example")
@UrlParameterMapping("example/:exampleId")
// Will match /example/12345 and call setExampleId(12345)
class MyView extends Div implements HasAbsoluteUrlParameterMapping {
    //                         note ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    @UrlParameter
    public Integer exampleId;
    
    ...
}
```

Regular expressions are supported:
```java
@Route("new-message")
@UrlParameterMapping(":userId")
...
@UrlParameter(regEx="[0-9]{1,6}")
Integer userId;
// Will match /new-message/123456, but not /new-message/1234567
```

Regular expressions could be also dynamic:
```java
import org.vaadin.flow.helper.*;

...

@Route("example")
@UrlParameterMapping(":selectedTab")
// Will match /example/12345 and call setExampleId(12345)
class MyView extends Div implements HasUrlParameterMapping {
    static {
        UrlParameterMappingHelper.setDynamicRegex(MyView.class, "selectedTab", MyView::getSelectedTabRegex);
    }

    @UrlParameter(dynamicRegEx = true)
    public String selectedTab;
   
    public String getSelectedTabRegex() {
        return String.join("|", backendService.getAvailableTabs());
    }    
    ...
}

```

Multiple mappings are supported:
```java
@Route("forum/thread")
@RouteAlias("forum/message")
@UrlParameterMapping("forum/thread/:threadId[/:urlTitle]")
@UrlParameterMapping("forum/thread/:threadId/:messageId")
@UrlParameterMapping("forum/message/:messageId")
// Will match (with HasAbsoluteUrlParameterMapping)
// - /forum/thread/12345
// - /forum/thread/12345/forum-post-title
// - /forum/thread/12345/67890
// - /forum/message/67890
```

It is also possible to check which of patterns matched:
```java
@Route(...)
@UrlParameterMapping(SomeView.ORDER_VIEW)
@UrlParameterMapping(SomeView.ORDER_EDIT)
class SomeView extends Div implements HasUrlParameterMapping {
    final static String ORDER_VIEW = ":orderId[/view]";
    final static String ORDER_EDIT = ":orderId/edit";

    @UrlMatchedPatternParameter()
    public String matchedPattern;

    @UrlParameter(name = "orderId")
    public void setOrder(Integer orderId) { ... }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if ( ORDER_VIEW.equals(matchedPattern) ) {
            ...
        } else {
            ...
        }
    }

    ...
}
```

URL parameter matching could also be used for Vaadin `RequestHandler`:
```java
class DownloadRequestHandler implements RequestHandler {
    @UrlParameterMapping("download/:uuid")
    public class ParameterMapping {
        @UrlParameter()
        public UUID uuid;
    }

    boolean handleRequest(VaadinSession session, VaadinRequest request,
                VaadinResponse response) throws IOException {
		VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        ParameterMapping mapping = new ParameterMapping();
		
        if ( UrlParameterMappingHelper.match(mapping, servletRequest.getRequestURI())) {
            return false;
        }
        
        ...
    }
}
```

If no matches are detected, automatic `rerouteToError(NotFoundException.class)` will
be performed. It's possible to use custom exception or view using `@RerouteIfNotMatched(...)` 
annotation, or disable this feature completely using `@IgnoreIfNotMatched` annotation.
In this case you can check if there were any matches using 
`@UrlMatchedPatternParameter` annotated field/setter.

When no custom regular expression is specified, it is automatically derived
from field/method type:
- String: `[^/]+` -- anything up to next slash
- Integer: `-?[0-1]?[0-9]{1,9}` -- `-1999999999` to `1999999999`
- Long: `-?[0-8]?[0-9]{1,18}` -- `-8999999999999999999` to `8999999999999999999`
- Boolean: `true|false`
- UUID: `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`

## Development instructions

Starting the test/demo server:
```
mvn jetty:run
```

This deploys demo at http://localhost:8080
