# URL Parameter Mapping for Flow

While https://github.com/vaadin/flow/issues/2740 and 
https://github.com/vaadin/flow/issues/4213 are still in the works, 
the need for flexible parametrized routes still exist. This
helper implementation lives on top of built in HasUrlParameter
and provides support for named parameters.

Simple usage example:
```java
import org.vaadin.flow.helper.*;

...

@Route("example")
@UrlParameterMapping(":exampleId")
// Will match /example/12345 and call setExampleId(12345)
class MyView extends Div implements HasUrlParameterMapping {
    private Integer exampleId;
    
    public void setExampleId(Integer exampleId) {
        // setExampleId(null) will be called if the route won't match
        this.exampleId = exampleId;
    }
    
    ...
}
```  

Optional parameters are supported:
```java
@Route("example")
@UrlParameterMapping(":exampleId[/:version]")
// Will match /example/12345 and /example/12345/678 
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
    private Integer exampleId;
    
    public void setExampleId(Integer exampleId) {
        // setExampleId(null) will be called if the route won't match
        this.exampleId = exampleId;
    }
    
    ...
}
```

Regular expressions are supported:
```java
@Route("new-message")
@UrlParameterMapping(":userId:[0-9]{1,6}:")
// Will match /new-message/123456
```

Multiple mappings are supported:
```java
@Route("forum/thread")
@RouteAlias("forum/message")
@UrlParameterMapping("forum/thread/:threadId[/:urlTitle:.*:]")
@UrlParameterMapping("forum/thread/:threadId/:messageId")
@UrlParameterMapping("forum/message/:messageId")
// Will match (with HasAbsoluteUrlParameterMapping)
// - /forum/thread/12345
// - /forum/thread/12345/forum-post-title
// - /forum/thread/12345/67890
// - /forum/message/67890
```

When no regular expression is specified, it is automatically derived
from property type:
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
