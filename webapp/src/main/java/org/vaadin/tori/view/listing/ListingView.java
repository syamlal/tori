/*
 * Copyright 2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.vaadin.tori.view.listing;

import org.vaadin.tori.component.PanicComponent;
import org.vaadin.tori.data.entity.Category;
import org.vaadin.tori.mvp.View;

public interface ListingView extends View {

    void displayCategoryNotFoundError(String requestedCategoryId);

    /**
     * Show an error message to the user that says that something irrecoverable
     * went wrong, and there's nothing really we can do.
     * 
     * @see PanicComponent
     */
    void panic();

    void setCategory(Category category);

    void showError(String message);

    void setThreadsVisible(boolean showThreads);

}
