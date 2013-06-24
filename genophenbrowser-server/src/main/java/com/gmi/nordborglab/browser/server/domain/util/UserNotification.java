package com.gmi.nordborglab.browser.server.domain.util;

import com.gmi.nordborglab.browser.server.domain.BaseEntity;
import com.gmi.nordborglab.browser.server.domain.acl.AppUser;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 4/17/13
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "user_notification", schema = "util")
@AttributeOverride(name = "id", column = @Column(name = "id"))
@SequenceGenerator(name = "idSequence", sequenceName = "util.user_notification_id_seq")
public class UserNotification extends BaseEntity {

    private String type;

    private String text;

    @Column(name = "create_date")
    private Date createDate = new Date();

    @Transient
    private boolean isRead = false;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    public UserNotification() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
        appUser.addUserNotification(this);
    }

    public boolean isRead(Date lastCheckDate) {
        if (lastCheckDate != null && lastCheckDate.getTime() > createDate.getTime()) {
            isRead = true;
        } else {
            isRead = false;
        }
        return isRead;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}
