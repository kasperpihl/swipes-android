package com.swipesapp.android.daogenerator;

import de.greenrobot.daogenerator.*;

/**
 * Generates entities and DAOs for the Swipes project.
 *
 * Run it as a Java application (not Android).
 *
 * @author Felipe Bari
 */
public class SwipesDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1000, "com.swipesapp.android.db");
        schema.enableKeepSectionsByDefault();

        addEntities(schema);

        new DaoGenerator().generateAll(schema, "../Swipes/swipes/src-gen/main/java");
    }

    private static void addEntities(Schema schema) {
        // Task table.
        Entity task = schema.addEntity("Task");
        task.addIdProperty();
        task.addStringProperty("objectId");
        task.addStringProperty("tempId");
        task.addStringProperty("parentLocalId");
        task.addDateProperty("createdAt");
        task.addDateProperty("updatedAt");
        task.addBooleanProperty("deleted");
        task.addStringProperty("title");
        task.addStringProperty("notes");
        task.addIntProperty("order");
        task.addIntProperty("priority");
        task.addDateProperty("completionDate");
        task.addDateProperty("schedule");
        task.addStringProperty("location");
        task.addDateProperty("repeatDate");
        task.addStringProperty("repeatOption");
        task.addStringProperty("origin");
        task.addStringProperty("originIdentifier");

        // Tag table.
        Entity tag = schema.addEntity("Tag");
        tag.addIdProperty();
        tag.addStringProperty("objectId");
        tag.addStringProperty("tempId");
        tag.addDateProperty("createdAt");
        tag.addDateProperty("updatedAt");
        tag.addStringProperty("title");

        // Attachment table.
        Entity attachment = schema.addEntity("Attachment");
        attachment.addIdProperty();
        attachment.addStringProperty("identifier");
        attachment.addStringProperty("service");
        attachment.addStringProperty("title");
        attachment.addBooleanProperty("sync");

        // Join table for tasks and tags.
        Entity taskTag = schema.addEntity("TaskTag");
        Property taskId = taskTag.addLongProperty("taskId").notNull().getProperty();
        Property tagId = taskTag.addLongProperty("tagId").notNull().getProperty();

        // Join table relation to Task.
        taskTag.addToOne(task, taskId);
        // Join table relation to Tag.
        taskTag.addToOne(tag, tagId);

        // Task relation to join table.
        ToMany taskToTags = task.addToMany(taskTag, taskId);
        taskToTags.setName("taskTags");

        // Tag relation to join table.
        ToMany tagToTasks = tag.addToMany(taskTag, tagId);
        tagToTasks.setName("taskTags");

        // To-Many relation of tasks and attachments.
        Property attTaskId = attachment.addLongProperty("taskId").notNull().getProperty();
        ToMany taskToAttachment = task.addToMany(attachment, attTaskId);
        taskToAttachment.setName("attachments");
    }

}
