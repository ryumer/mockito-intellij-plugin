**I stopped supporting the plugin, please feel free to fork the project.**

Intellij plugin for generation of Mockito code in unit tests

## Plugin
The following code elements are auto-generated by the plugin
  * `@RunWith(MockitoJUnitRunner.class)` annotation for the class
  * Mocked fields for each non-static object declared in the test subject
  * Field for the test subject with `@InjectMocks` annotation
  * static imports for useful mockito functions like `when`, `verify`

## Examples
#### Before code generation
    import org.junit.Test;
    
    class FooTest {
    
        @Test
        public void testFoo() throws Exception {
            
        }
  
    }

#### After code generation
    import org.junit.Test;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.runners.MockitoJUnitRunner;
    
    import static org.mockito.Mockito.*;
    
    @RunWith(MockitoJUnitRunner.class)
    class FooTest {
    
        @Mock
        private Random random;
    
        @InjectMocks
        private Foo underTest;
    
        @Test
        public void testFoo() throws Exception {
    
        }
    }

## Plugin Usage
  To generate the Mockito code with the plugin hit <code>ctrl shift M</code> or use a choose the action from
"Generate Action" context menu.
