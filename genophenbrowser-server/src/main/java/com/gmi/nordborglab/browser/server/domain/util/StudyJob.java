package com.gmi.nordborglab.browser.server.domain.util;

import com.gmi.nordborglab.browser.server.domain.BaseEntity;
import com.gmi.nordborglab.browser.server.domain.cdv.Study;

import javax.persistence.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 3/8/13
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */

@Entity
@Table(name="study_job",schema="util")
@AttributeOverride(name="id", column=@Column(name="id"))
@SequenceGenerator(name="idSequence", sequenceName="util.study_job_id_seq")
public class StudyJob extends BaseEntity{

    public StudyJob() {
    }

    @OneToOne(fetch = FetchType.LAZY,cascade={CascadeType.PERSIST,CascadeType.MERGE},optional =true)
    @JoinColumn(name = "div_study_id",nullable = false)
    private Study study;

    private String status;

    private String task;
    private Integer progress;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="create_date")
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="modification_date")
    private Date modificationDate;


    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }
}
