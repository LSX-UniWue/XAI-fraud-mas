
from datetime import datetime
from datetime import timedelta
import numpy
import pandas


def addDeltaTime(dk,minutes=0):
    dk['Erfassungsuhrzeit'] = [(datetime.strptime(row['Erfassungsuhrzeit'], '%H:%M:%S') + timedelta(minutes=minutes)).strftime('%H:%M:%S')  for index, row in dk.iterrows()]
    return dk

def main(datasource):
    df = pandas.read_csv(f'../ml-framework/data/erp_fraud/{datasource}', encoding='ISO-8859-1', dtype={'Sperrgrund Menge' : 'string'})
    df = df.fillna('nan')

    time_mapping = pandas.read_csv('time_mapping.csv', encoding='ISO-8859-1', sep='\t')

    def mapToDay(timestampArray):
        ret =[]
        for k in range(0, len(timestampArray)):
            for index, row in time_mapping.iterrows():
                if pandas.to_datetime(timestampArray[k]) <= datetime.strptime(row['TIMESTAMP'], '%H:%M:%S'):
                    ret.append(row['DAY'])
                    break
        return numpy.asarray(ret)

    subframe = df[df['Transaktionsart'] == 'Sachkontenbuchung']
    subframe = subframe[(subframe['Wertestring'] == 'RE01' )]
    subframe['RUN'] = ['Fraud2'+subframe.loc[subframe.index[i], 'Material'] for i in range(0, len(subframe.index))]

    subframeP = df[df['Transaktionsart'] == 'Sachkontenbuchung']
    subframeP = subframeP[(subframeP['Vorgangsart GL'] == 'SD00' )]

    def getGroups(dk, type, timedeltaX=0, labeltoplot='Material'):
        from datetime import timedelta
        plotmatrix = pandas.DataFrame(columns={'x', 'y', 'label'})
        x1 = [v for a, v in dk['Menge'].items()]
        x2 = [v for a, v in dk['Betrag Hauswaehr'].items()]
        y=[]
        z=[]
        if type == 0:
            plotmatrix = pandas.DataFrame(columns={'x', 'y', 'z', 'label'})
            for i in range(0, len(x1)):
                y.append(round(x2[i] / x1[i],2))
            z= x1
        if type == 1:
            for i in range(0, len(x1)):
                y.append(round(x2[i] / x1[i],2))
        elif type== 2:
            y=x1
        else :
            y= x2

        plotmatrix['y'] = numpy.asarray(y)
        plotmatrix['label'] = numpy.asarray([v for a, v in dk[labeltoplot].items()])
        if type == 0:
            plotmatrix['z'] = numpy.asarray(z)

        if type != 4:
            if timedelta == 0:
                plotmatrix['x'] = [datetime.strptime(x, '%H:%M:%S') for a, x in dk['Erfassungsuhrzeit'].items()]
            else:
                plotmatrix['x'] = [datetime.strptime(x, '%H:%M:%S')+timedelta(minutes=timedeltaX) for a, x in dk['Erfassungsuhrzeit'].items()]
        else :
            plotmatrix['x'] = x1

        groups = plotmatrix.groupby('label')
        return groups

    def doubleInterpolateNormalized(time, value, product=False):
        #print(len(time))
        time = mapToDay(time)
        #print(len(time))
        if product:
            k=0
            while k < len(time)-1 :  # length greater than 1
                jt=1
                while time[k] == time[jt+k]:
                    value[k] += value[jt+k]
                    jt=jt+1
                    if (k+jt) >= len(time):
                        break
                k = k+jt

        tmp = [value[0]]
        Ttmp=[time[0]]
        lastid = time[0]
        lastval = value[0]
        for k in range(1, len(time)):
            if time[k] == lastid:
                continue
            elif time[k] != (1 + lastid):
                dy = value[k] - lastval
                dx = time[k] - lastid
                for l in range(lastid + 1, time[k]):
                    linfunc = (dy / dx) * (l - lastid) + lastval
                    tmp.append(round(linfunc, 2))
                    Ttmp.append(l)
                tmp.append(value[k])
                Ttmp.append(time[k])
                lastid = time[k]
                lastval = value[k]
            else:
                lastid = time[k]
                lastval = value[k]
                tmp.append(value[k])
                Ttmp.append(time[k])
        # extrapolate
        tmp2=[]
        Ttmp2=[]
        if Ttmp[0] != 0:
            dx = Ttmp[1]-Ttmp[0]
            dy = tmp[1] -tmp[0]
            m = dy/dx
            b = tmp[0]- ( m )* Ttmp[0]
            for z in range(0, Ttmp[0]):
                Ttmp2.append(z)
                linf= max(m*z+b, 0)
                tmp2.append(round(linf,2))
            for z in range(0, len(Ttmp)):
                Ttmp2.append(Ttmp[z])
                tmp2.append(tmp[z])
        if len(Ttmp2) < 240:
            firstvalue = tmp2[0]
            lastvalue = tmp2[len(tmp2)-1]
            lasttime= Ttmp2[len(Ttmp2)-1]
            dx = 240-lasttime
            dy = firstvalue-lastvalue
            m= dy /dx
            b = lastvalue
            j= 1
            while((j+lasttime) < 240):
                Ttmp2.append((j+lasttime))
                tmp2.append(round(m*j+b, 2))
                j = j+1
        elif len(Ttmp2) > 240:
            Ttmp2 = Ttmp2[0:240]
            tmp2 = tmp2[0:240]

        return numpy.asarray(Ttmp2), numpy.asarray(tmp2)

    print('\tGenerating Educt Trends...')
    # save extracted trends of educts
    for name, group in getGroups(subframe, 1):
        print(f'\t\t{name}')
        filename = 'DailyPricesVolumes/' + str(name) + '-Price.arff'
        file = open(filename, 'w')
        file.write('% Title: ' + str(name) + '-Price Trend \n% Source: \'Julian Tritscher et al., Open ERP System Data For Occupational Fraud Detection, 2022\' \n@RELATION '+ str(name)+'-Time-Price \n@ATTRIBUTE time NUMERIC \n@ATTRIBUTE price NUMERIC \n@DATA\n')
        x,y = doubleInterpolateNormalized(numpy.asarray(group.x), numpy.asarray(group.y), product=False)
        for i in range(len(x)):
            if(i != len(x)-1):
                file.write(str(x[i]) + ',' + str(y[i]) + '\n')
            else:
                file.write(str(x[i]) + ',' + str(y[i]) )
        file.close()
    print('\tGenerating Product Trends...')
    # extract trends of products
    for name, group in getGroups(subframeP, 0):
        print(f'\t\t{name}')
        filename = 'DailyPricesVolumes/' + str(name) + '-Price.arff'
        file = open(filename, 'w')
        file.write('% Title: ' + str(name) + '-Price Trend \n% Source: \'Julian Tritscher et al., Open ERP System Data For Occupational Fraud Detection, 2022\' \n@RELATION '+ str(name)+'-Time-Price-Amount \n@ATTRIBUTE time NUMERIC\n@ATTRIBUTE price NUMERIC \n@ATTRIBUTE amount NUMERIC\n@DATA\n')
        file.write('')

        x, y = doubleInterpolateNormalized(numpy.asarray(group.x), numpy.asarray(group.y), product=True)
        x, z = doubleInterpolateNormalized(numpy.asarray(group.x), numpy.asarray(group.z), product=True)
        for i in range(len(x)):
            if i != len(x)-1:
                if z[i] != 0:
                    file.write(str(x[i]) + ',' + str(round(y[i]/z[i], 2)) + ',' + str(round(z[i])) + '\n')
                else:
                    file.write(str(x[i]) + ',' + str(0) + ',' + str(0) + '\n')
            else:
                if z[i] != 0:
                    file.write(str(x[i]) + ',' + str(round(y[i]/z[i], 2)) + ',' + str(round(z[i])))
                else:
                    file.write(str(x[i]) + ',' + str(0) + ',' + str(0))
        file.close()
