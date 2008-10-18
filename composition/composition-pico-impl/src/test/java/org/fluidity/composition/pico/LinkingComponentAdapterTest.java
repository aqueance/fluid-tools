/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition.pico;

import org.easymock.EasyMock;
import org.fluidity.tests.MockGroupAbstractTest;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoVisitor;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class LinkingComponentAdapterTest extends MockGroupAbstractTest {

    private final PicoContainer linkedPico = addControl(PicoContainer.class);

    private final PicoContainer enclosingPico = addControl(PicoContainer.class);

    private final ComponentAdapter linkedAdapter = addControl(ComponentAdapter.class);

    private final PicoVisitor visitor = addControl(PicoVisitor.class);

    @Test
    public void usesRightKey() throws Exception {
        final Object linkedKey = new Object();
        final Object newKey = new Object();

        replay();
        final LinkingComponentAdapter adapter = new LinkingComponentAdapter(linkedPico, newKey, linkedKey);
        assert newKey == adapter.getComponentKey();
        verify();
    }

    @Test
    public void invokesLinkedPicoForComponent() throws Exception {
        final Object linkedKey = new Object();
        final Object newKey = new Object();

        final Object component = new Object();
        EasyMock.expect(linkedPico.getComponentInstance(linkedKey)).andReturn(component);
        EasyMock.expect(linkedPico.getComponentInstance(linkedKey)).andReturn(null);

        replay();
        final LinkingComponentAdapter adapter = new LinkingComponentAdapter(linkedPico, newKey, linkedKey);
        assert component == adapter.getComponentInstance(enclosingPico);
        assert adapter.getComponentInstance(enclosingPico) == null;
        verify();
    }

    @Test
    public void invokesLinkedAdapterForImplementation() throws Exception {
        final Object linkedKey = new Object();
        final Object newKey = new Object();
        final Class componentClass = String.class;

        EasyMock.expect(linkedPico.getComponentAdapter(linkedKey)).andReturn(linkedAdapter);
        EasyMock.expect(linkedAdapter.getComponentImplementation()).andReturn(componentClass);

        replay();
        final LinkingComponentAdapter adapter = new LinkingComponentAdapter(linkedPico, newKey, linkedKey);
        assert componentClass == adapter.getComponentImplementation();
        verify();
    }

    @Test
    public void acceptsVisitor() throws Exception {
        final Object linkedKey = new Object();
        final Object newKey = new Object();

        EasyMock.expect(linkedPico.getComponentAdapter(linkedKey)).andReturn(linkedAdapter);
        linkedAdapter.accept(visitor);

        replay();
        final LinkingComponentAdapter adapter = new LinkingComponentAdapter(linkedPico, newKey, linkedKey);
        adapter.accept(visitor);
        verify();
    }

    @Test(expectedExceptions = LinkedComponentNotFoundException.class)
    public void verifiesComponent() throws Exception {
        final Object linkedKey = new Object();
        final Object newKey = new Object();

        EasyMock.expect(linkedPico.getComponentAdapter(linkedKey)).andReturn(linkedAdapter);
        EasyMock.expect(linkedPico.getComponentAdapter(linkedKey)).andReturn(null);

        replay();
        final LinkingComponentAdapter adapter = new LinkingComponentAdapter(linkedPico, newKey, linkedKey);
        adapter.verify(enclosingPico);
        try {
            adapter.verify(enclosingPico);
        } finally {
            verify();
        }
    }
}
