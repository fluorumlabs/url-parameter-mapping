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
   <version>1.0.0-alpha2</version>
</dependency>
```

Usage example:
```java
import org.vaadin.flow.helper.*;

...

@Route("example")
@UrlParameterMapping(":exampleId/:orderId")
// Will match /example/12345 and call setExampleId(12345)
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
    
    // exampleId will be null if the route won't match
    
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
 
     @UrlParameter(name = "orderId")
     public void setOrder(Integer orderId) { ... }
 
     @Override
     public void beforeEnter(BeforeEnterEvent event) {
         if ( isPatternMatched(ORDER_VIEW) ) {
             ...
         } else {
             ...
         }
     }
 
     ...
 }
```

If no matches are detected, automatic `rerouteToError(NotFoundException.class)` will
be performed. It's possible to use custom exception using `@RerouteIfNotMatched(...)` 
annotation, or disable this feature completely using `@IgnoreIfNotMatched` annotation.
In this case you can check if there were any matches using `isPatternMatched()` call.

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
